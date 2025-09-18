package com.czagrzebski.printhelm.web.repository;

import com.czagrzebski.printhelm.web.domain.connection.ConnectionConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectionConfigRepository extends JpaRepository<ConnectionConfig, Long> {

}