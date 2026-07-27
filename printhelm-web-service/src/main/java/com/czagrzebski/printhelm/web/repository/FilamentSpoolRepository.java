package com.czagrzebski.printhelm.web.repository;

import com.czagrzebski.printhelm.web.domain.FilamentSpool;
import com.czagrzebski.printhelm.web.domain.FilamentSpoolStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FilamentSpoolRepository extends JpaRepository<FilamentSpool, Long> {

    List<FilamentSpool> findAllByOrderByCreatedAtDesc();

    List<FilamentSpool> findByStatus(FilamentSpoolStatus status);
}
