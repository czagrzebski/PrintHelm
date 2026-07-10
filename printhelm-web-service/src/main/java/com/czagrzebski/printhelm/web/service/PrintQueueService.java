package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.model.ApiJobOrderResponse;
import com.czagrzebski.printhelm.model.ApiQueueReorderEntry;
import com.czagrzebski.printhelm.web.domain.GcodeMetadata;
import com.czagrzebski.printhelm.web.domain.JobOrder;
import com.czagrzebski.printhelm.web.domain.JobOrderFileVersion;
import com.czagrzebski.printhelm.web.domain.JobOrderStatus;
import com.czagrzebski.printhelm.web.domain.JobOrderVersionFile;
import com.czagrzebski.printhelm.web.domain.PrintOutcome;
import com.czagrzebski.printhelm.web.domain.Printer;
import com.czagrzebski.printhelm.web.domain.notification.NotificationSeverity;
import com.czagrzebski.printhelm.web.domain.notification.NotificationType;
import com.czagrzebski.printhelm.web.mapper.JobOrderMapper;
import com.czagrzebski.printhelm.web.repository.GcodeMetadataRepository;
import com.czagrzebski.printhelm.web.repository.JobOrderFileVersionRepository;
import com.czagrzebski.printhelm.web.repository.JobOrderRepository;
import com.czagrzebski.printhelm.web.repository.PrinterRepository;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Transactional
public class PrintQueueService {

    private static final Logger logger = LogManager.getLogger(PrintQueueService.class);

    private final JobOrderRepository jobOrderRepository;
    private final PrinterRepository printerRepository;
    private final PrinterCommandService printerCommandService;
    private final JobOrderMapper jobOrderMapper;
    private final PrinterFileService printerFileService;
    private final JobOrderFileService jobOrderFileService;
    private final NotificationService notificationService;
    private final JobOrderService jobOrderService;
    private final JobOrderFileVersionRepository fileVersionRepository;
    private final GcodeMetadataRepository gcodeMetadataRepository;
    private final PrintHistoryService printHistoryService;
    private final FilamentSpoolService filamentSpoolService;
    private final AuditLogService auditLogService;

    public PrintQueueService(JobOrderRepository jobOrderRepository,
                             PrinterRepository printerRepository,
                             PrinterCommandService printerCommandService,
                             JobOrderMapper jobOrderMapper,
                             PrinterFileService printerFileService,
                             JobOrderFileService jobOrderFileService,
                             NotificationService notificationService,
                             JobOrderService jobOrderService,
                             JobOrderFileVersionRepository fileVersionRepository,
                             GcodeMetadataRepository gcodeMetadataRepository,
                             PrintHistoryService printHistoryService,
                             FilamentSpoolService filamentSpoolService,
                             AuditLogService auditLogService) {
        this.jobOrderRepository = jobOrderRepository;
        this.printerRepository = printerRepository;
        this.printerCommandService = printerCommandService;
        this.jobOrderMapper = jobOrderMapper;
        this.printerFileService = printerFileService;
        this.jobOrderFileService = jobOrderFileService;
        this.notificationService = notificationService;
        this.jobOrderService = jobOrderService;
        this.fileVersionRepository = fileVersionRepository;
        this.gcodeMetadataRepository = gcodeMetadataRepository;
        this.printHistoryService = printHistoryService;
        this.filamentSpoolService = filamentSpoolService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<ApiJobOrderResponse> getQueue(long printerId) {
        findPrinterOrThrow(printerId);
        return jobOrderMapper.jobOrdersToApiJobOrderResponses(
                jobOrderRepository.findByAssignedPrinter_PrinterIdOrderByQueuePositionAsc(printerId));
    }

    public ApiJobOrderResponse addToQueue(long printerId, long jobOrderId) {
        Printer printer = findPrinterOrThrow(printerId);
        JobOrder job = findJobOrThrow(jobOrderId);

        if (job.getStatus() != JobOrderStatus.READY_TO_PRINT) {
            throw new IllegalArgumentException("Job must be in READY_TO_PRINT status to be queued");
        }
        if (job.getAssignedPrinter() != null) {
            throw new IllegalStateException("Job is already assigned to a printer");
        }
        if (job.getMongoGcodeFileId() == null) {
            throw new IllegalArgumentException("Job has no gcode file uploaded");
        }

        String filename = job.getGcodeFilename();
        ensureFileOnPrinter(printerId, filename, job.getMongoGcodeFileId());

        int nextPosition = jobOrderRepository.findMaxQueuePosition(printerId) + 1;
        job.setAssignedPrinter(printer);
        job.setAssignedFilename(filename);
        job.setQueuePosition(nextPosition);

        JobOrder saved = jobOrderRepository.save(job);

        notificationService.createNotification(
                printerId,
                printer.getPrinterName(),
                NotificationType.JOB_QUEUED,
                NotificationSeverity.INFO,
                "Job queued — select colors to start",
                "Job #" + job.getOrderId() + " assigned to " + printer.getPrinterName() + ". Open the print queue to select AMS colors and start the print.",
                job.getGcodeFilename()
        );

        auditLogService.record("JOB_QUEUED", "JobOrder", job.getOrderId(),
                "Queued on " + printer.getPrinterName() + " (position " + nextPosition + ", file " + filename + ")");

        return jobOrderMapper.jobOrderToApiJobOrderResponse(saved);
    }

    private void ensureFileOnPrinter(long printerId, String filename, String mongoGcodeFileId) {
        try {
            boolean alreadyPresent = printerFileService.listFiles(printerId)
                    .stream().anyMatch(f -> f.name().equals(filename));
            if (!alreadyPresent) {
                try (InputStream stream = jobOrderFileService.getFileResource(mongoGcodeFileId).getInputStream()) {
                    printerFileService.uploadFile(printerId, filename, stream);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to prepare file on printer: " + e.getMessage(), e);
        }
    }

    public void removeFromQueue(long printerId, long jobOrderId) throws MqttException {
        findPrinterOrThrow(printerId);
        JobOrder job = findJobOrThrow(jobOrderId);

        if (job.getAssignedPrinter() == null || job.getAssignedPrinter().getPrinterId() != printerId) {
            throw new IllegalArgumentException("Job is not assigned to printer " + printerId);
        }

        if (job.getStatus() == JobOrderStatus.PRINTING) {
            try {
                printerCommandService.stopPrint(printerId);
            } catch (Exception e) {
                logger.warn("Could not send stop command to printer [ID={}] while removing job [ID={}]: {}", printerId, jobOrderId, e.getMessage());
            }
            printHistoryService.recordPrintEnd(job, PrintOutcome.CANCELED, null);
            job.setPrintStartedAt(null);
        }

        String printerName = job.getAssignedPrinter().getPrinterName();
        job.setStatus(JobOrderStatus.READY_TO_PRINT);
        job.setAssignedPrinter(null);
        job.setAssignedFilename(null);
        job.setQueuePosition(null);
        jobOrderRepository.save(job);

        auditLogService.record("JOB_UNQUEUED", "JobOrder", jobOrderId, "Removed from queue of " + printerName);
    }

    public void startJob(long printerId, long jobOrderId, int[] amsMapping,
                         boolean flowCali, boolean vibrationCali, boolean layerInspect) throws MqttException {
        findPrinterOrThrow(printerId);
        JobOrder job = findJobOrThrow(jobOrderId);

        if (job.getAssignedPrinter() == null || job.getAssignedPrinter().getPrinterId() != printerId) {
            throw new IllegalArgumentException("Job is not assigned to printer " + printerId);
        }
        if (job.getStatus() != JobOrderStatus.READY_TO_PRINT) {
            throw new IllegalArgumentException("Job must be in READY_TO_PRINT status to start printing");
        }
        if (jobOrderRepository.existsByAssignedPrinter_PrinterIdAndStatus(printerId, JobOrderStatus.PRINTING)) {
            throw new IllegalStateException("Printer already has an active print job");
        }

        printerCommandService.printFile(printerId, job.getAssignedFilename(), amsMapping, flowCali, vibrationCali, layerInspect);
        job.setStatus(JobOrderStatus.PRINTING);
        job.setPrintStartedAt(LocalDateTime.now());
        jobOrderRepository.save(job);

        auditLogService.record("PRINT_STARTED", "JobOrder", jobOrderId,
                "Started " + job.getAssignedFilename() + " on " + job.getAssignedPrinter().getPrinterName());
    }

    public void reorderQueue(long printerId, List<ApiQueueReorderEntry> entries) {
        findPrinterOrThrow(printerId);
        List<JobOrder> queuedJobs = jobOrderRepository.findByAssignedPrinter_PrinterIdOrderByQueuePositionAsc(printerId);

        for (ApiQueueReorderEntry entry : entries) {
            queuedJobs.stream()
                    .filter(j -> j.getOrderId().equals(entry.getJobOrderId()))
                    .findFirst()
                    .ifPresentOrElse(
                            j -> j.setQueuePosition(entry.getPosition()),
                            () -> { throw new IllegalArgumentException("Job " + entry.getJobOrderId() + " is not in this printer's queue"); }
                    );
        }

        jobOrderRepository.saveAll(queuedJobs);
        auditLogService.record("QUEUE_REORDERED", "Printer", printerId,
                "Reordered " + entries.size() + " queued job(s)");
    }

    /**
     * Called when the printer reports FINISH for the job's current file. Records the completed
     * copy against the print plan (per-file quantities on the active gcode version) and either
     * requeues the order for the next copy/file or marks it PRINT_FINISHED when the plan is done.
     * Orders without version records (legacy) complete immediately, as before.
     */
    public void completePrintTask(JobOrder job) {
        String printerName = job.getAssignedPrinter() != null ? job.getAssignedPrinter().getPrinterName() : null;
        Long printerId = job.getAssignedPrinter() != null ? job.getAssignedPrinter().getPrinterId() : null;

        recordCompletedPrint(job);

        JobOrderFileVersion version = jobOrderService.findGcodeVersionContainingFile(
                job.getOrderId(), job.getMongoGcodeFileId());
        if (version == null) {
            job.setStatus(JobOrderStatus.PRINT_FINISHED);
            jobOrderRepository.save(job);
            return;
        }

        if (version.getFiles() == null || version.getFiles().isEmpty()) {
            version.setFiles(new ArrayList<>(version.getEffectiveFiles()));
        }
        List<JobOrderVersionFile> files = version.getFiles();
        files.stream()
                .filter(f -> Objects.equals(f.getMongoFileId(), job.getMongoGcodeFileId()))
                .findFirst()
                .ifPresent(f -> f.setCompletedCount(f.effectiveCompleted() + 1));
        fileVersionRepository.save(version);

        int totalPrints = files.stream().mapToInt(JobOrderVersionFile::effectiveQuantity).sum();
        int completedPrints = files.stream()
                .mapToInt(f -> Math.min(f.effectiveCompleted(), f.effectiveQuantity()))
                .sum();

        // Next incomplete file: prefer finishing the current file's remaining copies first
        JobOrderVersionFile next = files.stream()
                .filter(f -> Objects.equals(f.getMongoFileId(), job.getMongoGcodeFileId()))
                .filter(f -> !f.isPlanComplete())
                .findFirst()
                .orElseGet(() -> files.stream().filter(f -> !f.isPlanComplete()).findFirst().orElse(null));

        if (next == null) {
            job.setStatus(JobOrderStatus.PRINT_FINISHED);
            jobOrderRepository.save(job);
            return;
        }

        job.setMongoGcodeFileId(next.getMongoFileId());
        job.setGcodeFilename(next.getFilename());
        job.setMongoGcodeMetadataId(next.getMongoGcodeMetadataId());
        job.setStatus(JobOrderStatus.READY_TO_PRINT);
        job.setAssignedPrinter(null);
        job.setAssignedFilename(null);
        job.setQueuePosition(null);
        job.setPrintStartedAt(null);
        jobOrderRepository.save(job);
        logger.info("Job [ID={}] print {} of {} complete; requeued for next file [{}]",
                job.getOrderId(), completedPrints, totalPrints, next.getFilename());

        if (printerId != null) {
            notificationService.createNotification(
                    printerId,
                    printerName,
                    NotificationType.PRINT_COMPLETED,
                    NotificationSeverity.INFO,
                    "Print " + completedPrints + " of " + totalPrints + " complete",
                    "Job #" + job.getOrderId() + " finished a print on " + printerName
                            + ". Next up: " + next.getFilename() + " — the job has been requeued.",
                    next.getFilename()
            );
        }
    }

    /**
     * Writes the print-history row and deducts filament usage from matching inventory
     * spools while the printer assignment and gcode pointers are still on the job.
     */
    private void recordCompletedPrint(JobOrder job) {
        GcodeMetadata metadata = job.getMongoGcodeMetadataId() != null
                ? gcodeMetadataRepository.findById(job.getMongoGcodeMetadataId()).orElse(null)
                : null;

        Double gramsUsed = null;
        if (metadata != null) {
            if (metadata.getTotalWeightGrams() != null) {
                gramsUsed = metadata.getTotalWeightGrams();
            } else if (metadata.getFilaments() != null) {
                double sum = metadata.getFilaments().stream()
                        .filter(f -> f.getUsedGrams() != null)
                        .mapToDouble(f -> f.getUsedGrams()).sum();
                gramsUsed = sum > 0 ? sum : null;
            }
        }

        printHistoryService.recordPrintEnd(job, PrintOutcome.COMPLETED, gramsUsed);

        try {
            filamentSpoolService.consumeForCompletedPrint(job.getAssignedPrinter(), metadata,
                    "job #" + job.getOrderId() + " (" + job.getGcodeFilename() + ")");
        } catch (Exception e) {
            logger.warn("Filament deduction failed for job [ID={}]: {}", job.getOrderId(), e.getMessage());
        }
    }

    private Printer findPrinterOrThrow(long printerId) {
        return printerRepository.findById(printerId)
                .orElseThrow(() -> new IllegalArgumentException("Printer not found: " + printerId));
    }

    private JobOrder findJobOrThrow(long jobOrderId) {
        return jobOrderRepository.findById(jobOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Job order not found: " + jobOrderId));
    }
}
