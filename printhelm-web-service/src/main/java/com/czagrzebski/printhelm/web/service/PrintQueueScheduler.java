package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.model.ApiMaterial;
import com.czagrzebski.printhelm.model.ApiPrinterState;
import com.czagrzebski.printhelm.web.domain.GcodeFilamentInfo;
import com.czagrzebski.printhelm.web.domain.GcodeMetadata;
import com.czagrzebski.printhelm.web.domain.JobOrder;
import com.czagrzebski.printhelm.web.domain.JobOrderStatus;
import com.czagrzebski.printhelm.web.domain.PrintOutcome;
import com.czagrzebski.printhelm.web.domain.Printer;
import com.czagrzebski.printhelm.web.repository.GcodeMetadataRepository;
import com.czagrzebski.printhelm.web.repository.JobOrderRepository;
import com.czagrzebski.printhelm.web.repository.PrinterRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class PrintQueueScheduler {

    private static final Logger logger = LogManager.getLogger(PrintQueueScheduler.class);

    private static final Set<String> IDLE_STATES = Set.of("IDLE", "FINISH", "FAILED");
    private static final Set<String> BUSY_STATES = Set.of("RUNNING", "PAUSE", "PREPARE");
    private static final Set<String> CANCEL_STATES = Set.of("IDLE", "FAILED");
    private static final long PRINT_START_GRACE_SECONDS = 90;

    private final PrinterRepository printerRepository;
    private final JobOrderRepository jobOrderRepository;
    private final PrintQueueService printQueueService;
    private final PrinterStateCache printerStateCache;
    private final GcodeMetadataRepository gcodeMetadataRepository;
    private final PrintHistoryService printHistoryService;

    public PrintQueueScheduler(PrinterRepository printerRepository,
                               JobOrderRepository jobOrderRepository,
                               PrintQueueService printQueueService,
                               PrinterStateCache printerStateCache,
                               GcodeMetadataRepository gcodeMetadataRepository,
                               PrintHistoryService printHistoryService) {
        this.printerRepository = printerRepository;
        this.jobOrderRepository = jobOrderRepository;
        this.printQueueService = printQueueService;
        this.printerStateCache = printerStateCache;
        this.gcodeMetadataRepository = gcodeMetadataRepository;
        this.printHistoryService = printHistoryService;
    }

    @Scheduled(fixedDelay = 15_000)
    public void tick() {
        for (Printer printer : printerRepository.findAll()) {
            try {
                processPrinter(printer);
            } catch (Exception e) {
                logger.warn("PrintQueueScheduler: error processing printer [ID={}]: {}", printer.getPrinterId(), e.getMessage());
            }
        }
    }

    public void processPrinter(Printer printer) {
        long printerId = printer.getPrinterId();

        String gcodeState = printerStateCache.getState(printerId)
                .map(s -> s.getGcodeState() != null ? s.getGcodeState().toUpperCase() : null)
                .orElse(null);

        // Skip printers we've never heard from or that are actively busy
        if (gcodeState == null || BUSY_STATES.contains(gcodeState)) return;
        if (!IDLE_STATES.contains(gcodeState)) return;

        // Handle any lingering PRINTING jobs based on how the printer stopped
        LocalDateTime graceCutoff = LocalDateTime.now().minusSeconds(PRINT_START_GRACE_SECONDS);
        List<JobOrder> printingJobs = jobOrderRepository
                .findByAssignedPrinter_PrinterIdAndStatus(printerId, JobOrderStatus.PRINTING);
        for (JobOrder job : printingJobs) {
            // Guard against acting on a job that was just started — the printer's gcodeState may
            // still be stale in the cache (IDLE, or FINISH from the previous copy) while the print
            // is actually launching. Only complete or revert after the grace period has elapsed.
            if (job.getPrintStartedAt() != null && job.getPrintStartedAt().isAfter(graceCutoff)) {
                continue;
            }
            if ("FINISH".equals(gcodeState)) {
                // Records the finished copy against the order's print plan; the order only
                // becomes PRINT_FINISHED once every file × quantity has been printed.
                printQueueService.completePrintTask(job);
                logger.info("Recorded finished print for job [ID={}] on printer [ID={}] (status now {})",
                        job.getOrderId(), printerId, job.getStatus());
            } else if (CANCEL_STATES.contains(gcodeState)) {
                printHistoryService.recordPrintEnd(job,
                        "FAILED".equals(gcodeState) ? PrintOutcome.FAILED : PrintOutcome.CANCELED, null);
                job.setStatus(JobOrderStatus.READY_TO_PRINT);
                job.setAssignedPrinter(null);
                job.setAssignedFilename(null);
                job.setQueuePosition(null);
                job.setPrintStartedAt(null);
                jobOrderRepository.save(job);
                logger.info("Reverted job [ID={}] to READY_TO_PRINT after cancelled/failed print on printer [ID={}]", job.getOrderId(), printerId);
            }
        }

        // Don't assign a new job if the printer already has one queued (status = READY_TO_PRINT)
        boolean hasQueuedJob = jobOrderRepository
                .existsByAssignedPrinter_PrinterIdAndStatus(printerId, JobOrderStatus.READY_TO_PRINT);
        if (hasQueuedJob) return;

        // Find the oldest unassigned READY_TO_PRINT job whose filament requirements the
        // printer can satisfy with its currently loaded AMS materials
        List<JobOrder> candidates = jobOrderRepository
                .findByStatusAndAssignedPrinterIsNullOrderByCreatedAtAsc(JobOrderStatus.READY_TO_PRINT);
        for (JobOrder job : candidates) {
            if (!isCompatibleWithPrinter(job, printerId)) {
                logger.debug("Skipping job [ID={}] for printer [ID={}]: loaded filament does not match requirements",
                        job.getOrderId(), printerId);
                continue;
            }
            logger.info("Auto-assigning job [ID={}] to printer [ID={}]", job.getOrderId(), printerId);
            printQueueService.addToQueue(printerId, job.getOrderId());
            return;
        }
    }

    /**
     * A job is compatible when every filament its gcode requires (type + color) is loaded in
     * the printer's material system. Jobs without parsed filament metadata and printers that
     * don't report loaded materials (no AMS) fall back to the legacy behavior of accepting
     * any job.
     */
    private boolean isCompatibleWithPrinter(JobOrder job, long printerId) {
        if (job.getMongoGcodeMetadataId() == null) return true;
        GcodeMetadata metadata = gcodeMetadataRepository.findById(job.getMongoGcodeMetadataId()).orElse(null);
        if (metadata == null || metadata.getFilaments() == null || metadata.getFilaments().isEmpty()) return true;

        List<ApiMaterial> loadedMaterials = printerStateCache.getState(printerId)
                .map(ApiPrinterState::getMaterialSystem)
                .map(ms -> ms.getMaterials())
                .orElse(null);
        if (loadedMaterials == null || loadedMaterials.isEmpty()) return true;

        for (GcodeFilamentInfo required : metadata.getFilaments()) {
            // "Loaded" on a material means it is currently feeding the extruder, so at most one
            // tray is ever loaded — compatibility only requires the filament to be present in
            // a tray (empty slots report no type and never match).
            boolean satisfied = loadedMaterials.stream()
                    .anyMatch(m -> typeMatches(m.getType(), required.getType())
                            && FilamentSpoolService.colorMatches(m.getColor(), required.getColor()));
            if (!satisfied) return false;
        }
        return true;
    }

    private boolean typeMatches(String loaded, String required) {
        return loaded != null && required != null && loaded.trim().equalsIgnoreCase(required.trim());
    }
}
