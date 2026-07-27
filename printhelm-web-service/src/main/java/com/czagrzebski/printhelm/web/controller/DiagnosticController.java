package com.czagrzebski.printhelm.web.controller;

import com.czagrzebski.printhelm.model.ApiDiagnosticReport;
import com.czagrzebski.printhelm.web.ai.PrinterDiagnosticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/printer")
public class DiagnosticController {

    @Autowired
    private PrinterDiagnosticService diagnosticService;

    @PostMapping("/{id}/diagnose")
    public ResponseEntity<ApiDiagnosticReport> diagnose(@PathVariable long id) {
        try {
            return ResponseEntity.ok(diagnosticService.diagnose(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
