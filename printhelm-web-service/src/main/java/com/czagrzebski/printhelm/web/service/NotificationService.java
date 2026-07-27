package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.web.domain.notification.NotificationSeverity;
import com.czagrzebski.printhelm.web.domain.notification.NotificationType;
import com.czagrzebski.printhelm.web.domain.notification.PrinterNotification;
import com.czagrzebski.printhelm.web.repository.PrinterNotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final PrinterNotificationRepository repository;
    private final SimpMessagingTemplate messagingTemplate;

    public NotificationService(PrinterNotificationRepository repository, SimpMessagingTemplate messagingTemplate) {
        this.repository = repository;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public PrinterNotification createNotification(Long printerId, String printerName, NotificationType type,
                                                   NotificationSeverity severity, String title, String message, String file) {
        PrinterNotification notification = new PrinterNotification();
        notification.setPrinterId(printerId);
        notification.setPrinterName(printerName);
        notification.setType(type);
        notification.setSeverity(severity);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setFile(file);

        PrinterNotification saved = repository.save(notification);
        messagingTemplate.convertAndSend("/topic/notifications", saved);
        return saved;
    }

    public List<PrinterNotification> getAllNotifications() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public List<PrinterNotification> getNotificationsByPrinter(Long printerId) {
        return repository.findByPrinterIdOrderByCreatedAtDesc(printerId);
    }

    public long getUnreadCount() {
        return repository.countByAcknowledgedFalse();
    }

    @Transactional
    public void acknowledgeNotification(Long id) {
        repository.findById(id).ifPresent(n -> {
            n.setAcknowledged(true);
            repository.save(n);
        });
    }

    @Transactional
    public void acknowledgeAll() {
        repository.acknowledgeAll();
    }

    @Transactional
    public void deleteNotification(Long id) {
        repository.deleteById(id);
    }

    @Transactional
    public void clearAcknowledged() {
        repository.deleteAllAcknowledged();
    }
}
