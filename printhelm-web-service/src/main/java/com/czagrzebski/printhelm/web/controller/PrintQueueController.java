package com.czagrzebski.printhelm.web.controller;

import com.czagrzebski.printhelm.model.ApiJobOrderResponse;
import com.czagrzebski.printhelm.model.ApiQueueJobRequest;
import com.czagrzebski.printhelm.model.ApiQueueReorderRequest;
import com.czagrzebski.printhelm.model.ApiQueueStartRequest;
import com.czagrzebski.printhelm.web.service.PrintQueueService;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/printer/{printerId}/queue")
public class PrintQueueController {

    private final PrintQueueService printQueueService;

    public PrintQueueController(PrintQueueService printQueueService) {
        this.printQueueService = printQueueService;
    }

    @GetMapping
    public ResponseEntity<List<ApiJobOrderResponse>> getQueue(@PathVariable long printerId) {
        return ResponseEntity.ok(printQueueService.getQueue(printerId));
    }

    @PostMapping
    public ResponseEntity<ApiJobOrderResponse> addToQueue(@PathVariable long printerId,
                                                          @RequestBody ApiQueueJobRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(printQueueService.addToQueue(printerId, request.getJobOrderId()));
    }

    @DeleteMapping("/{jobOrderId}")
    public ResponseEntity<Void> removeFromQueue(@PathVariable long printerId,
                                                @PathVariable long jobOrderId) {
        printQueueService.removeFromQueue(printerId, jobOrderId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{jobOrderId}/start")
    public ResponseEntity<Void> startJob(@PathVariable long printerId,
                                         @PathVariable long jobOrderId,
                                         @RequestBody ApiQueueStartRequest request) throws MqttException {
        printQueueService.startJob(printerId, jobOrderId,
                request.getAmsMapping().stream().mapToInt(Integer::intValue).toArray(),
                request.getFlowCali(), request.getVibrationCali(), request.getLayerInspect());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping
    public ResponseEntity<Void> reorderQueue(@PathVariable long printerId,
                                             @RequestBody ApiQueueReorderRequest request) {
        printQueueService.reorderQueue(printerId, request.getEntries());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleConflict(IllegalStateException ex) {
        return ResponseEntity.status(409).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
