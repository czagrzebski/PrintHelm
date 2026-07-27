package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.web.domain.JobOrder;
import com.czagrzebski.printhelm.web.domain.PrintHistory;
import com.czagrzebski.printhelm.web.domain.PrintOutcome;
import com.czagrzebski.printhelm.web.domain.Printer;
import com.czagrzebski.printhelm.web.repository.PrintHistoryRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class PrintHistoryService {

    private static final Logger logger = LogManager.getLogger(PrintHistoryService.class);

    private final PrintHistoryRepository printHistoryRepository;

    public PrintHistoryService(PrintHistoryRepository printHistoryRepository) {
        this.printHistoryRepository = printHistoryRepository;
    }

    /**
     * Records the end of a print attempt. Must never break queue processing, so failures
     * are logged and swallowed.
     */
    public void recordPrintEnd(JobOrder job, PrintOutcome outcome, Double filamentGramsUsed) {
        try {
            Printer printer = job.getAssignedPrinter();
            LocalDateTime now = LocalDateTime.now();

            PrintHistory history = new PrintHistory();
            history.setPrinterId(printer != null ? printer.getPrinterId() : null);
            history.setPrinterName(printer != null ? printer.getPrinterName() : null);
            history.setOrderId(job.getOrderId());
            history.setFilename(job.getGcodeFilename());
            history.setStartedAt(job.getPrintStartedAt());
            history.setFinishedAt(now);
            if (job.getPrintStartedAt() != null) {
                history.setDurationMinutes((int) Duration.between(job.getPrintStartedAt(), now).toMinutes());
            }
            history.setOutcome(outcome);
            history.setFilamentGramsUsed(filamentGramsUsed);
            printHistoryRepository.save(history);
        } catch (Exception e) {
            logger.warn("Failed to record print history for job [ID={}]: {}", job.getOrderId(), e.getMessage());
        }
    }
}
