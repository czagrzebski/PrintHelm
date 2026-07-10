package com.czagrzebski.printhelm.web.repository;

import com.czagrzebski.printhelm.web.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    @Query("""
            SELECT a FROM AuditLog a
            WHERE (:entityType IS NULL OR a.entityType = :entityType)
              AND (:username IS NULL OR a.username = :username)
              AND (:action IS NULL OR a.action = :action)
            ORDER BY a.timestamp DESC, a.auditId DESC
            """)
    Page<AuditLog> search(@Param("entityType") String entityType,
                          @Param("username") String username,
                          @Param("action") String action,
                          Pageable pageable);
}
