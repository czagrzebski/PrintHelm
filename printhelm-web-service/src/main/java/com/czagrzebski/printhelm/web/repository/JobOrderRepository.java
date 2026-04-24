package com.czagrzebski.printhelm.web.repository;

import com.czagrzebski.printhelm.web.domain.JobOrder;
import com.czagrzebski.printhelm.web.domain.JobOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface JobOrderRepository extends JpaRepository<JobOrder, Long> {

    List<JobOrder> findByAssignedPrinter_PrinterIdOrderByQueuePositionAsc(long printerId);

    boolean existsByAssignedPrinter_PrinterIdAndStatus(long printerId, JobOrderStatus status);

    @Query("SELECT COALESCE(MAX(j.queuePosition), 0) FROM JobOrder j WHERE j.assignedPrinter.printerId = :printerId")
    int findMaxQueuePosition(@Param("printerId") long printerId);

    Optional<JobOrder> findTopByStatusAndAssignedPrinterIsNullOrderByCreatedAtAsc(JobOrderStatus status);

    List<JobOrder> findByAssignedPrinter_PrinterIdAndStatus(long printerId, JobOrderStatus status);
}
