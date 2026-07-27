package com.czagrzebski.printhelm.web.controller;

import com.czagrzebski.printhelm.model.ApiBusinessSettings;
import com.czagrzebski.printhelm.web.domain.BusinessSettings;
import com.czagrzebski.printhelm.web.service.BusinessSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings/business")
public class BusinessSettingsController {

    private final BusinessSettingsService businessSettingsService;

    public BusinessSettingsController(BusinessSettingsService businessSettingsService) {
        this.businessSettingsService = businessSettingsService;
    }

    @GetMapping
    public ResponseEntity<ApiBusinessSettings> getBusinessSettings() {
        BusinessSettings settings = businessSettingsService.getSettings();
        ApiBusinessSettings response = toApiModel(settings);
        return ResponseEntity.ok(response);
    }

    @PutMapping
    public ResponseEntity<ApiBusinessSettings> updateBusinessSettings(@RequestBody ApiBusinessSettings request) {
        BusinessSettings updated = businessSettingsService.updateSettings(
                request.getBusinessName(),
                request.getBusinessAddress(),
                request.getBusinessEmail(),
                request.getBusinessPhone()
        );
        return ResponseEntity.ok(toApiModel(updated));
    }

    private ApiBusinessSettings toApiModel(BusinessSettings settings) {
        ApiBusinessSettings api = new ApiBusinessSettings();
        api.setBusinessName(settings.getBusinessName());
        api.setBusinessAddress(settings.getBusinessAddress());
        api.setBusinessEmail(settings.getBusinessEmail());
        api.setBusinessPhone(settings.getBusinessPhone());
        return api;
    }
}
