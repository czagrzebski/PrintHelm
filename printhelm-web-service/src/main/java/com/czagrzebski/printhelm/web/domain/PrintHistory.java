package com.czagrzebski.printhelm.web.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One row per finished print attempt (completed, failed, or canceled). Printer identity is
 * denormalized so history survives printer deletion.
 */
@Entity
@Table(name = "PrintHistory", indexes = {
        @Index(name = "idx_print_history_finished", columnList = "finished_at"),
        @Index(name = "idx_print_history_printer", columnList = "printer_id")
})
public class PrintHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "print_history_id")
    private Long printHistoryId;

    @Column(name = "printer_id")
    private Long printerId;

    @Column(name = "printer_name", length = 100)
    private String printerName;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "filename", length = 255)
    private String filename;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at", nullable = false)
    private LocalDateTime finishedAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", nullable = false, columnDefinition = "varchar(20)")
    private PrintOutcome outcome;

    @Column(name = "filament_grams_used")
    private Double filamentGramsUsed;

    public Long getPrintHistoryId() { return printHistoryId; }
    public void setPrintHistoryId(Long printHistoryId) { this.printHistoryId = printHistoryId; }

    public Long getPrinterId() { return printerId; }
    public void setPrinterId(Long printerId) { this.printerId = printerId; }

    public String getPrinterName() { return printerName; }
    public void setPrinterName(String printerName) { this.printerName = printerName; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public PrintOutcome getOutcome() { return outcome; }
    public void setOutcome(PrintOutcome outcome) { this.outcome = outcome; }

    public Double getFilamentGramsUsed() { return filamentGramsUsed; }
    public void setFilamentGramsUsed(Double filamentGramsUsed) { this.filamentGramsUsed = filamentGramsUsed; }
}
