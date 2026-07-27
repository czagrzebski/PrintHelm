package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.model.ApiAnalyticsSummary;
import com.czagrzebski.printhelm.model.ApiAnalyticsTrendPoint;
import com.czagrzebski.printhelm.model.ApiPrinterAnalytics;
import com.czagrzebski.printhelm.web.domain.JobOrder;
import com.czagrzebski.printhelm.web.domain.JobOrderStatus;
import com.czagrzebski.printhelm.web.domain.PrintHistory;
import com.czagrzebski.printhelm.web.domain.PrintOutcome;
import com.czagrzebski.printhelm.web.domain.Printer;
import com.czagrzebski.printhelm.web.repository.JobOrderRepository;
import com.czagrzebski.printhelm.web.repository.PrintHistoryRepository;
import com.czagrzebski.printhelm.web.repository.PrinterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    private final PrintHistoryRepository printHistoryRepository;
    private final JobOrderRepository jobOrderRepository;
    private final PrinterRepository printerRepository;

    public AnalyticsService(PrintHistoryRepository printHistoryRepository,
                            JobOrderRepository jobOrderRepository,
                            PrinterRepository printerRepository) {
        this.printHistoryRepository = printHistoryRepository;
        this.jobOrderRepository = jobOrderRepository;
        this.printerRepository = printerRepository;
    }

    public ApiAnalyticsSummary getSummary(int days) {
        LocalDateTime cutoff = cutoff(days);
        List<PrintHistory> history = printHistoryRepository.findByFinishedAtAfter(cutoff);

        long completed = history.stream().filter(h -> h.getOutcome() == PrintOutcome.COMPLETED).count();
        long failed = history.stream().filter(h -> h.getOutcome() == PrintOutcome.FAILED).count();
        long canceled = history.stream().filter(h -> h.getOutcome() == PrintOutcome.CANCELED).count();
        long totalMinutes = history.stream()
                .filter(h -> h.getDurationMinutes() != null)
                .mapToLong(PrintHistory::getDurationMinutes).sum();
        double gramsUsed = history.stream()
                .filter(h -> h.getFilamentGramsUsed() != null)
                .mapToDouble(PrintHistory::getFilamentGramsUsed).sum();

        List<JobOrder> orders = jobOrderRepository.findAll();
        Map<String, Long> ordersByStatus = new HashMap<>();
        for (JobOrder order : orders) {
            ordersByStatus.merge(order.getStatus().name(), 1L, Long::sum);
        }
        long openOrders = orders.stream()
                .filter(o -> o.getStatus() != JobOrderStatus.PRINT_FINISHED && o.getStatus() != JobOrderStatus.INVOICED)
                .count();

        double revenue = 0;
        long invoicedOrders = 0;
        for (JobOrder order : orders) {
            if (order.getInvoicedAt() == null || order.getInvoicedAt().isBefore(cutoff)) continue;
            invoicedOrders++;
            revenue += amount(order.getMaterialCost()) + amount(order.getLaborCost())
                    + amount(order.getSetupFee()) - amount(order.getDiscount());
        }

        long finishedAttempts = completed + failed + canceled;
        ApiAnalyticsSummary summary = new ApiAnalyticsSummary();
        summary.setDays(days);
        summary.setPrintsCompleted(completed);
        summary.setPrintsFailed(failed);
        summary.setPrintsCanceled(canceled);
        summary.setSuccessRate(finishedAttempts > 0 ? (double) completed / finishedAttempts : null);
        summary.setTotalPrintMinutes(totalMinutes);
        summary.setFilamentGramsUsed(gramsUsed);
        summary.setRevenue(revenue);
        summary.setInvoicedOrders(invoicedOrders);
        summary.setOpenOrders(openOrders);
        summary.setOrdersByStatus(ordersByStatus);
        return summary;
    }

    public List<ApiPrinterAnalytics> getPrinterAnalytics(int days) {
        List<PrintHistory> history = printHistoryRepository.findByFinishedAtAfter(cutoff(days));

        // Seed with all registered printers so idle ones still show up with zeros
        Map<Long, ApiPrinterAnalytics> byPrinter = new LinkedHashMap<>();
        for (Printer printer : printerRepository.findAll()) {
            byPrinter.put(printer.getPrinterId(), newPrinterEntry(printer.getPrinterId(), printer.getPrinterName()));
        }

        for (PrintHistory entry : history) {
            Long printerId = entry.getPrinterId();
            if (printerId == null) continue;
            ApiPrinterAnalytics stats = byPrinter.computeIfAbsent(printerId,
                    id -> newPrinterEntry(id, entry.getPrinterName() != null ? entry.getPrinterName() : "Printer " + id));
            switch (entry.getOutcome()) {
                case COMPLETED -> stats.setPrintsCompleted(stats.getPrintsCompleted() + 1);
                case FAILED -> stats.setPrintsFailed(stats.getPrintsFailed() + 1);
                case CANCELED -> stats.setPrintsCanceled(stats.getPrintsCanceled() + 1);
            }
            if (entry.getDurationMinutes() != null) {
                stats.setTotalPrintMinutes(stats.getTotalPrintMinutes() + entry.getDurationMinutes());
            }
            if (entry.getFilamentGramsUsed() != null) {
                stats.setFilamentGramsUsed(stats.getFilamentGramsUsed() + entry.getFilamentGramsUsed());
            }
        }

        for (ApiPrinterAnalytics stats : byPrinter.values()) {
            long attempts = stats.getPrintsCompleted() + stats.getPrintsFailed() + stats.getPrintsCanceled();
            stats.setSuccessRate(attempts > 0 ? (double) stats.getPrintsCompleted() / attempts : null);
        }
        return new ArrayList<>(byPrinter.values());
    }

    public List<ApiAnalyticsTrendPoint> getTrends(int days) {
        LocalDateTime cutoff = cutoff(days);
        List<PrintHistory> history = printHistoryRepository.findByFinishedAtAfter(cutoff);

        Map<LocalDate, ApiAnalyticsTrendPoint> byDay = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (LocalDate day = today.minusDays(days - 1L); !day.isAfter(today); day = day.plusDays(1)) {
            ApiAnalyticsTrendPoint point = new ApiAnalyticsTrendPoint();
            point.setDate(day);
            point.setPrintsCompleted(0L);
            point.setPrintsFailed(0L);
            point.setPrintMinutes(0L);
            point.setFilamentGramsUsed(0.0);
            byDay.put(day, point);
        }

        for (PrintHistory entry : history) {
            ApiAnalyticsTrendPoint point = byDay.get(entry.getFinishedAt().toLocalDate());
            if (point == null) continue;
            if (entry.getOutcome() == PrintOutcome.COMPLETED) {
                point.setPrintsCompleted(point.getPrintsCompleted() + 1);
            } else {
                point.setPrintsFailed(point.getPrintsFailed() + 1);
            }
            if (entry.getDurationMinutes() != null) {
                point.setPrintMinutes(point.getPrintMinutes() + entry.getDurationMinutes());
            }
            if (entry.getFilamentGramsUsed() != null) {
                point.setFilamentGramsUsed(point.getFilamentGramsUsed() + entry.getFilamentGramsUsed());
            }
        }
        return new ArrayList<>(byDay.values());
    }

    private ApiPrinterAnalytics newPrinterEntry(long printerId, String printerName) {
        ApiPrinterAnalytics stats = new ApiPrinterAnalytics();
        stats.setPrinterId(printerId);
        stats.setPrinterName(printerName);
        stats.setPrintsCompleted(0L);
        stats.setPrintsFailed(0L);
        stats.setPrintsCanceled(0L);
        stats.setTotalPrintMinutes(0L);
        stats.setFilamentGramsUsed(0.0);
        return stats;
    }

    private LocalDateTime cutoff(int days) {
        return LocalDate.now().minusDays(days - 1L).atStartOfDay();
    }

    private double amount(BigDecimal value) {
        return value != null ? value.doubleValue() : 0;
    }
}
