package com.czagrzebski.printhelm.web.repository;

import com.czagrzebski.printhelm.web.domain.PrintHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PrintHistoryRepository extends JpaRepository<PrintHistory, Long> {

    List<PrintHistory> findByFinishedAtAfter(LocalDateTime cutoff);
}
