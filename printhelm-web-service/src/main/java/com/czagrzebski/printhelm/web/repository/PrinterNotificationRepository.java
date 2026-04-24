package com.czagrzebski.printhelm.web.repository;

import com.czagrzebski.printhelm.web.domain.notification.PrinterNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrinterNotificationRepository extends JpaRepository<PrinterNotification, Long> {

    List<PrinterNotification> findAllByOrderByCreatedAtDesc();

    List<PrinterNotification> findByPrinterIdOrderByCreatedAtDesc(Long printerId);

    long countByAcknowledgedFalse();

    @Modifying
    @Query("UPDATE PrinterNotification n SET n.acknowledged = true WHERE n.acknowledged = false")
    void acknowledgeAll();

    @Modifying
    @Query("DELETE FROM PrinterNotification n WHERE n.acknowledged = true")
    void deleteAllAcknowledged();
}
