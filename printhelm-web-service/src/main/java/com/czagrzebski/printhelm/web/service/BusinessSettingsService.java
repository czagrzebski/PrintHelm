package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.web.domain.BusinessSettings;
import com.czagrzebski.printhelm.web.repository.BusinessSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessSettingsService {

    private final BusinessSettingsRepository repository;
    private final AuditLogService auditLogService;

    public BusinessSettingsService(BusinessSettingsRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    public BusinessSettings getSettings() {
        return repository.findById(1L).orElseGet(() -> {
            BusinessSettings defaults = new BusinessSettings();
            return repository.save(defaults);
        });
    }

    @Transactional
    public BusinessSettings updateSettings(String businessName, String businessAddress,
                                            String businessEmail, String businessPhone) {
        BusinessSettings settings = getSettings();
        settings.setBusinessName(businessName);
        settings.setBusinessAddress(businessAddress);
        settings.setBusinessEmail(businessEmail);
        settings.setBusinessPhone(businessPhone);
        BusinessSettings saved = repository.save(settings);
        auditLogService.record("SETTINGS_UPDATED", "BusinessSettings", saved.getId(), businessName);
        return saved;
    }
}
