package com.czagrzebski.printhelm.web.repository;

import com.czagrzebski.printhelm.web.domain.JobOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobOrderRepository extends JpaRepository<JobOrder, Long> {
}
