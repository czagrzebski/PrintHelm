package com.czagrzebski.printhelm.web.domain.notification;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "printer_notification")
public class PrinterNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "printer_id", nullable = false)
    private Long printerId;

    @Column(name = "printer_name")
    private String printerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(50)")
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(50)")
    private NotificationSeverity severity;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String message;

    @Column(name = "print_file")
    private String file;

    @Column(nullable = false)
    private boolean acknowledged = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPrinterId() { return printerId; }
    public void setPrinterId(Long printerId) { this.printerId = printerId; }

    public String getPrinterName() { return printerName; }
    public void setPrinterName(String printerName) { this.printerName = printerName; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public NotificationSeverity getSeverity() { return severity; }
    public void setSeverity(NotificationSeverity severity) { this.severity = severity; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getFile() { return file; }
    public void setFile(String file) { this.file = file; }

    public boolean isAcknowledged() { return acknowledged; }
    public void setAcknowledged(boolean acknowledged) { this.acknowledged = acknowledged; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
