package com.czagrzebski.printhelm.web.controller;

import com.czagrzebski.printhelm.model.ApiFilamentSpool;
import com.czagrzebski.printhelm.model.ApiFilamentSpoolRequest;
import com.czagrzebski.printhelm.web.service.FilamentSpoolService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/filament/spools")
public class FilamentSpoolController {

    private final FilamentSpoolService filamentSpoolService;

    public FilamentSpoolController(FilamentSpoolService filamentSpoolService) {
        this.filamentSpoolService = filamentSpoolService;
    }

    @GetMapping
    public ResponseEntity<List<ApiFilamentSpool>> getSpools() {
        return ResponseEntity.ok(filamentSpoolService.getAllSpools());
    }

    @PostMapping
    public ResponseEntity<ApiFilamentSpool> createSpool(@RequestBody ApiFilamentSpoolRequest request) {
        return ResponseEntity.status(201).body(filamentSpoolService.createSpool(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiFilamentSpool> updateSpool(@PathVariable long id,
                                                        @RequestBody ApiFilamentSpoolRequest request) {
        return ResponseEntity.ok(filamentSpoolService.updateSpool(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpool(@PathVariable long id) {
        filamentSpoolService.deleteSpool(id);
        return ResponseEntity.noContent().build();
    }
}
