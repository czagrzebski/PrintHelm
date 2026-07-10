package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.model.ApiAuditLogEntry;
import com.czagrzebski.printhelm.model.ApiAuditLogPage;
import com.czagrzebski.printhelm.web.domain.AuditLog;
import com.czagrzebski.printhelm.web.repository.AuditLogRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.stream.Collectors;

@Service
public class AuditLogService {

    private static final Logger logger = LogManager.getLogger(AuditLogService.class);

    public static final String SYSTEM_USER = "system";

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Records an audit entry attributed to the current authenticated user, or "system"
     * for background work (e.g. the print queue scheduler). Persisting the entry must
     * never break the operation being audited, so failures are logged and swallowed.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String entityType, Object entityId, String details) {
        try {
            AuditLog entry = new AuditLog();
            entry.setUsername(currentUsername());
            entry.setAction(action);
            entry.setEntityType(entityType);
            entry.setEntityId(entityId != null ? String.valueOf(entityId) : null);
            entry.setDetails(details != null && details.length() > 1000 ? details.substring(0, 1000) : details);
            auditLogRepository.save(entry);
        } catch (Exception e) {
            logger.warn("Failed to write audit entry [{} {} {}]: {}", action, entityType, entityId, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public ApiAuditLogPage search(int page, int size, String entityType, String username, String action) {
        Page<AuditLog> result = auditLogRepository.search(
                blankToNull(entityType), blankToNull(username), blankToNull(action),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200)));

        ApiAuditLogPage response = new ApiAuditLogPage();
        response.setPage(result.getNumber());
        response.setSize(result.getSize());
        response.setTotalElements(result.getTotalElements());
        response.setTotalPages(result.getTotalPages());
        response.setEntries(result.getContent().stream().map(this::toApi).collect(Collectors.toList()));
        return response;
    }

    private ApiAuditLogEntry toApi(AuditLog entry) {
        ApiAuditLogEntry api = new ApiAuditLogEntry();
        api.setAuditId(entry.getAuditId());
        api.setTimestamp(entry.getTimestamp() != null ? entry.getTimestamp().atOffset(ZoneOffset.UTC) : null);
        api.setUsername(entry.getUsername());
        api.setAction(entry.getAction());
        api.setEntityType(entry.getEntityType());
        api.setEntityId(entry.getEntityId());
        api.setDetails(entry.getDetails());
        return api;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return SYSTEM_USER;
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails userDetails) return userDetails.getUsername();
        if (principal instanceof String name && !"anonymousUser".equals(name)) return name;
        return SYSTEM_USER;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
