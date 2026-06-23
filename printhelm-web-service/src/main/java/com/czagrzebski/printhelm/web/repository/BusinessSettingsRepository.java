package com.czagrzebski.printhelm.web.repository;

import com.czagrzebski.printhelm.web.domain.BusinessSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessSettingsRepository extends JpaRepository<BusinessSettings, Long> {
}
