package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.model.ApiFilamentSpool;
import com.czagrzebski.printhelm.model.ApiFilamentSpoolRequest;
import com.czagrzebski.printhelm.model.FilamentSpoolStatus;
import com.czagrzebski.printhelm.web.domain.FilamentSpool;
import com.czagrzebski.printhelm.web.domain.GcodeFilamentInfo;
import com.czagrzebski.printhelm.web.domain.GcodeMetadata;
import com.czagrzebski.printhelm.web.domain.Printer;
import com.czagrzebski.printhelm.web.domain.notification.NotificationSeverity;
import com.czagrzebski.printhelm.web.domain.notification.NotificationType;
import com.czagrzebski.printhelm.web.repository.FilamentSpoolRepository;
import com.czagrzebski.printhelm.web.repository.PrinterRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional
public class FilamentSpoolService {

    private static final Logger logger = LogManager.getLogger(FilamentSpoolService.class);

    private final FilamentSpoolRepository spoolRepository;
    private final PrinterRepository printerRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public FilamentSpoolService(FilamentSpoolRepository spoolRepository,
                                PrinterRepository printerRepository,
                                NotificationService notificationService,
                                AuditLogService auditLogService) {
        this.spoolRepository = spoolRepository;
        this.printerRepository = printerRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<ApiFilamentSpool> getAllSpools() {
        return spoolRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toApi)
                .collect(Collectors.toList());
    }

    public ApiFilamentSpool createSpool(ApiFilamentSpoolRequest request) {
        FilamentSpool spool = new FilamentSpool();
        applyRequest(spool, request);
        if (spool.getRemainingWeightGrams() == null) {
            spool.setRemainingWeightGrams(spool.getInitialWeightGrams());
        }
        FilamentSpool saved = spoolRepository.save(spool);
        auditLogService.record("SPOOL_CREATED", "FilamentSpool", saved.getSpoolId(),
                saved.getName() + " (" + saved.getMaterial() + ", " + gramsLabel(saved.getRemainingWeightGrams()) + ")");
        return toApi(saved);
    }

    public ApiFilamentSpool updateSpool(long spoolId, ApiFilamentSpoolRequest request) {
        FilamentSpool spool = findOrThrow(spoolId);
        applyRequest(spool, request);
        FilamentSpool saved = spoolRepository.save(spool);
        auditLogService.record("SPOOL_UPDATED", "FilamentSpool", spoolId,
                saved.getName() + " (" + saved.getMaterial() + ", " + gramsLabel(saved.getRemainingWeightGrams()) + " left)");
        return toApi(saved);
    }

    public void deleteSpool(long spoolId) {
        FilamentSpool spool = findOrThrow(spoolId);
        spoolRepository.delete(spool);
        auditLogService.record("SPOOL_DELETED", "FilamentSpool", spoolId, spool.getName());
    }

    /**
     * Deducts filament used by a finished print from matching spools. Matching is by
     * material type + color among ACTIVE spools, preferring spools assigned to the printer
     * that ran the job. Returns the total grams deducted (0 when nothing matched or the
     * gcode metadata has no per-filament weights).
     */
    public double consumeForCompletedPrint(Printer printer, GcodeMetadata metadata, String jobLabel) {
        if (metadata == null || metadata.getFilaments() == null) return 0;

        double totalConsumed = 0;
        for (GcodeFilamentInfo filament : metadata.getFilaments()) {
            Double grams = filament.getUsedGrams();
            if (grams == null || grams <= 0) continue;

            FilamentSpool spool = findBestMatch(printer, filament);
            if (spool == null) {
                logger.info("No matching spool for {} {} ({}g) used by {}",
                        filament.getType(), filament.getColor(), grams, jobLabel);
                continue;
            }

            double before = spool.getRemainingWeightGrams() != null ? spool.getRemainingWeightGrams() : 0;
            double after = Math.max(0, before - grams);
            spool.setRemainingWeightGrams(after);
            totalConsumed += grams;

            Long printerId = printer != null ? printer.getPrinterId() : null;
            String printerName = printer != null ? printer.getPrinterName() : null;
            if (after <= 0) {
                spool.setStatus(com.czagrzebski.printhelm.web.domain.FilamentSpoolStatus.EMPTY);
                notificationService.createNotification(printerId, printerName,
                        NotificationType.FILAMENT_EMPTY, NotificationSeverity.WARNING,
                        "Filament spool empty",
                        "Spool \"" + spool.getName() + "\" (" + spool.getMaterial() + ") ran out after printing " + jobLabel + ".",
                        null);
            } else if (crossedLowThreshold(spool, before, after)) {
                notificationService.createNotification(printerId, printerName,
                        NotificationType.FILAMENT_LOW, NotificationSeverity.WARNING,
                        "Filament running low",
                        "Spool \"" + spool.getName() + "\" (" + spool.getMaterial() + ") is down to "
                                + gramsLabel(after) + " — below its " + gramsLabel(spool.getLowStockThresholdGrams()) + " threshold.",
                        null);
            }
            spoolRepository.save(spool);
            logger.info("Deducted {}g from spool [ID={} \"{}\"] for {} ({}g remaining)",
                    grams, spool.getSpoolId(), spool.getName(), jobLabel, String.format(Locale.ROOT, "%.1f", after));
        }
        return totalConsumed;
    }

    private boolean crossedLowThreshold(FilamentSpool spool, double before, double after) {
        Double threshold = spool.getLowStockThresholdGrams();
        return threshold != null && threshold > 0 && before > threshold && after <= threshold;
    }

    private FilamentSpool findBestMatch(Printer printer, GcodeFilamentInfo filament) {
        Long printerId = printer != null ? printer.getPrinterId() : null;
        List<FilamentSpool> candidates = spoolRepository
                .findByStatus(com.czagrzebski.printhelm.web.domain.FilamentSpoolStatus.ACTIVE).stream()
                .filter(s -> materialMatches(s.getMaterial(), filament.getType()))
                .filter(s -> colorMatches(s.getColorHex(), filament.getColor()))
                .toList();

        // Prefer spools loaded on the printer that ran the job, then spools with the most
        // filament left so partial spools aren't drained below zero unnecessarily.
        return candidates.stream()
                .max(Comparator
                        .comparing((FilamentSpool s) -> s.getAssignedPrinter() != null
                                && Objects.equals(s.getAssignedPrinter().getPrinterId(), printerId))
                        .thenComparing(s -> s.getRemainingWeightGrams() != null ? s.getRemainingWeightGrams() : 0))
                .orElse(null);
    }

    private boolean materialMatches(String spoolMaterial, String requiredType) {
        return spoolMaterial != null && requiredType != null
                && spoolMaterial.trim().equalsIgnoreCase(requiredType.trim());
    }

    /** Compares hex colors ignoring '#', case, and a trailing alpha channel (e.g. 43403DFF). */
    public static boolean colorMatches(String a, String b) {
        String na = normalizeColor(a);
        String nb = normalizeColor(b);
        return na != null && na.equals(nb);
    }

    private static String normalizeColor(String color) {
        if (color == null) return null;
        String hex = color.trim().replace("#", "").toUpperCase(Locale.ROOT);
        if (hex.length() == 8) hex = hex.substring(0, 6);
        return hex.length() == 6 ? hex : null;
    }

    private void applyRequest(FilamentSpool spool, ApiFilamentSpoolRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("Spool name is required");
        }
        if (request.getMaterial() == null || request.getMaterial().isBlank()) {
            throw new IllegalArgumentException("Spool material is required");
        }
        if (request.getInitialWeightGrams() == null || request.getInitialWeightGrams() <= 0) {
            throw new IllegalArgumentException("Initial weight must be greater than 0");
        }
        spool.setName(request.getName().trim());
        spool.setBrand(request.getBrand());
        spool.setMaterial(request.getMaterial().trim());
        spool.setColorHex(request.getColorHex());
        spool.setColorName(request.getColorName());
        if (request.getDiameter() != null) spool.setDiameter(request.getDiameter());
        spool.setInitialWeightGrams(request.getInitialWeightGrams());
        if (request.getRemainingWeightGrams() != null) {
            spool.setRemainingWeightGrams(Math.max(0, request.getRemainingWeightGrams()));
        }
        if (request.getSpoolCost() != null) {
            spool.setSpoolCost(BigDecimal.valueOf(request.getSpoolCost()));
        }
        if (request.getLowStockThresholdGrams() != null) {
            spool.setLowStockThresholdGrams(request.getLowStockThresholdGrams());
        }
        if (request.getStatus() != null) {
            spool.setStatus(com.czagrzebski.printhelm.web.domain.FilamentSpoolStatus.valueOf(request.getStatus().getValue()));
        }
        if (request.getAssignedPrinterId() != null) {
            Printer printer = printerRepository.findById(request.getAssignedPrinterId())
                    .orElseThrow(() -> new IllegalArgumentException("Printer not found: " + request.getAssignedPrinterId()));
            spool.setAssignedPrinter(printer);
        } else {
            spool.setAssignedPrinter(null);
        }
        spool.setAmsSlot(request.getAmsSlot());
        spool.setNotes(request.getNotes());
    }

    private ApiFilamentSpool toApi(FilamentSpool spool) {
        ApiFilamentSpool api = new ApiFilamentSpool();
        api.setSpoolId(spool.getSpoolId());
        api.setName(spool.getName());
        api.setBrand(spool.getBrand());
        api.setMaterial(spool.getMaterial());
        api.setColorHex(spool.getColorHex());
        api.setColorName(spool.getColorName());
        api.setDiameter(spool.getDiameter());
        api.setInitialWeightGrams(spool.getInitialWeightGrams());
        api.setRemainingWeightGrams(spool.getRemainingWeightGrams());
        api.setSpoolCost(spool.getSpoolCost() != null ? spool.getSpoolCost().doubleValue() : null);
        api.setLowStockThresholdGrams(spool.getLowStockThresholdGrams());
        api.setStatus(FilamentSpoolStatus.valueOf(spool.getStatus().name()));
        api.setAssignedPrinterId(spool.getAssignedPrinter() != null ? spool.getAssignedPrinter().getPrinterId() : null);
        api.setAssignedPrinterName(spool.getAssignedPrinter() != null ? spool.getAssignedPrinter().getPrinterName() : null);
        api.setAmsSlot(spool.getAmsSlot());
        api.setNotes(spool.getNotes());
        api.setCreatedAt(spool.getCreatedAt() != null ? spool.getCreatedAt().atOffset(ZoneOffset.UTC) : null);
        api.setUpdatedAt(spool.getUpdatedAt() != null ? spool.getUpdatedAt().atOffset(ZoneOffset.UTC) : null);
        boolean low = spool.getStatus() == com.czagrzebski.printhelm.web.domain.FilamentSpoolStatus.ACTIVE
                && spool.getLowStockThresholdGrams() != null
                && spool.getRemainingWeightGrams() != null
                && spool.getRemainingWeightGrams() <= spool.getLowStockThresholdGrams();
        api.setLowStock(low);
        return api;
    }

    private String gramsLabel(Double grams) {
        return grams == null ? "?g" : String.format(Locale.ROOT, "%.0fg", grams);
    }

    private FilamentSpool findOrThrow(long spoolId) {
        return spoolRepository.findById(spoolId)
                .orElseThrow(() -> new IllegalArgumentException("Filament spool not found: " + spoolId));
    }
}
