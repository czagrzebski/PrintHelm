package com.czagrzebski.printhelm.web.controller;

import com.czagrzebski.printhelm.web.domain.notification.PrinterNotification;
import com.czagrzebski.printhelm.web.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<PrinterNotification>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    @GetMapping("/printer/{printerId}")
    public ResponseEntity<List<PrinterNotification>> getNotificationsByPrinter(@PathVariable Long printerId) {
        return ResponseEntity.ok(notificationService.getNotificationsByPrinter(printerId));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount()));
    }

    @PutMapping("/{id}/acknowledge")
    public ResponseEntity<Void> acknowledgeNotification(@PathVariable Long id) {
        notificationService.acknowledgeNotification(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/acknowledge-all")
    public ResponseEntity<Void> acknowledgeAll() {
        notificationService.acknowledgeAll();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(@PathVariable Long id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/clear-acknowledged")
    public ResponseEntity<Void> clearAcknowledged() {
        notificationService.clearAcknowledged();
        return ResponseEntity.noContent().build();
    }
}
