package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.model.ApiJobOrderResponse;
import com.czagrzebski.printhelm.model.ApiQueueReorderEntry;
import com.czagrzebski.printhelm.web.domain.JobOrder;
import com.czagrzebski.printhelm.web.domain.JobOrderStatus;
import com.czagrzebski.printhelm.web.domain.Printer;
import com.czagrzebski.printhelm.web.domain.notification.NotificationSeverity;
import com.czagrzebski.printhelm.web.domain.notification.NotificationType;
import com.czagrzebski.printhelm.web.mapper.JobOrderMapper;
import com.czagrzebski.printhelm.web.repository.JobOrderRepository;
import com.czagrzebski.printhelm.web.repository.PrinterRepository;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@Transactional
public class PrintQueueService {

    private final JobOrderRepository jobOrderRepository;
    private final PrinterRepository printerRepository;
    private final PrinterCommandService printerCommandService;
    private final JobOrderMapper jobOrderMapper;
    private final PrinterFileService printerFileService;
    private final JobOrderFileService jobOrderFileService;
    private final NotificationService notificationService;

    public PrintQueueService(JobOrderRepository jobOrderRepository,
                             PrinterRepository printerRepository,
                             PrinterCommandService printerCommandService,
                             JobOrderMapper jobOrderMapper,
                             PrinterFileService printerFileService,
                             JobOrderFileService jobOrderFileService,
                             NotificationService notificationService) {
        this.jobOrderRepository = jobOrderRepository;
        this.printerRepository = printerRepository;
        this.printerCommandService = printerCommandService;
        this.jobOrderMapper = jobOrderMapper;
        this.printerFileService = printerFileService;
        this.jobOrderFileService = jobOrderFileService;
        this.notificationService = notificationService;
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

    public void removeFromQueue(long printerId, long jobOrderId) {
        findPrinterOrThrow(printerId);
        JobOrder job = findJobOrThrow(jobOrderId);

        if (job.getAssignedPrinter() == null || job.getAssignedPrinter().getPrinterId() != printerId) {
            throw new IllegalArgumentException("Job is not assigned to printer " + printerId);
        }

        job.setAssignedPrinter(null);
        job.setAssignedFilename(null);
        job.setQueuePosition(null);
        jobOrderRepository.save(job);
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
        jobOrderRepository.save(job);
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
