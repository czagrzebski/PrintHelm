package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.web.domain.JobOrder;
import com.czagrzebski.printhelm.web.domain.JobOrderStatus;
import com.czagrzebski.printhelm.web.domain.Printer;
import com.czagrzebski.printhelm.web.repository.JobOrderRepository;
import com.czagrzebski.printhelm.web.repository.PrinterRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class PrintQueueScheduler {

    private static final Logger logger = LogManager.getLogger(PrintQueueScheduler.class);

    private static final Set<String> IDLE_STATES = Set.of("IDLE", "FINISH", "FAILED");
    private static final Set<String> BUSY_STATES = Set.of("RUNNING", "PAUSE", "PREPARE");
    private static final Set<String> CANCEL_STATES = Set.of("IDLE", "FAILED");

    private final PrinterRepository printerRepository;
    private final JobOrderRepository jobOrderRepository;
    private final PrintQueueService printQueueService;
    private final PrinterStateCache printerStateCache;

    public PrintQueueScheduler(PrinterRepository printerRepository,
                               JobOrderRepository jobOrderRepository,
                               PrintQueueService printQueueService,
                               PrinterStateCache printerStateCache) {
        this.printerRepository = printerRepository;
        this.jobOrderRepository = jobOrderRepository;
        this.printQueueService = printQueueService;
        this.printerStateCache = printerStateCache;
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
        List<JobOrder> printingJobs = jobOrderRepository
                .findByAssignedPrinter_PrinterIdAndStatus(printerId, JobOrderStatus.PRINTING);
        for (JobOrder job : printingJobs) {
            if ("FINISH".equals(gcodeState)) {
                job.setStatus(JobOrderStatus.PRINT_FINISHED);
                jobOrderRepository.save(job);
                logger.info("Marked job [ID={}] as PRINT_FINISHED for printer [ID={}]", job.getOrderId(), printerId);
            } else if (CANCEL_STATES.contains(gcodeState)) {
                job.setStatus(JobOrderStatus.READY_TO_PRINT);
                job.setAssignedPrinter(null);
                job.setAssignedFilename(null);
                job.setQueuePosition(null);
                jobOrderRepository.save(job);
                logger.info("Reverted job [ID={}] to READY_TO_PRINT after cancelled/failed print on printer [ID={}]", job.getOrderId(), printerId);
            }
        }

        // Don't assign a new job if the printer already has one queued (status = READY_TO_PRINT)
        boolean hasQueuedJob = jobOrderRepository
                .existsByAssignedPrinter_PrinterIdAndStatus(printerId, JobOrderStatus.READY_TO_PRINT);
        if (hasQueuedJob) return;

        // Find the oldest unassigned READY_TO_PRINT job
        Optional<JobOrder> nextJobOpt = jobOrderRepository
                .findTopByStatusAndAssignedPrinterIsNullOrderByCreatedAtAsc(JobOrderStatus.READY_TO_PRINT);
        if (nextJobOpt.isEmpty()) return;

        JobOrder nextJob = nextJobOpt.get();
        logger.info("Auto-assigning job [ID={}] to printer [ID={}]", nextJob.getOrderId(), printerId);

        printQueueService.addToQueue(printerId, nextJob.getOrderId());
    }
}
