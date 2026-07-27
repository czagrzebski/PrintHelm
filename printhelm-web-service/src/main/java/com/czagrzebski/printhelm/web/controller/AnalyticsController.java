package com.czagrzebski.printhelm.web.controller;

import com.czagrzebski.printhelm.model.ApiAnalyticsSummary;
import com.czagrzebski.printhelm.model.ApiAnalyticsTrendPoint;
import com.czagrzebski.printhelm.model.ApiPrinterAnalytics;
import com.czagrzebski.printhelm.web.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final int MAX_DAYS = 365;

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiAnalyticsSummary> getSummary(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.getSummary(clamp(days)));
    }

    @GetMapping("/printers")
    public ResponseEntity<List<ApiPrinterAnalytics>> getPrinterAnalytics(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.getPrinterAnalytics(clamp(days)));
    }

    @GetMapping("/trends")
    public ResponseEntity<List<ApiAnalyticsTrendPoint>> getTrends(@RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.getTrends(clamp(days)));
    }

    private int clamp(int days) {
        return Math.min(Math.max(days, 1), MAX_DAYS);
    }
}
