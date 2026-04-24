package com.czagrzebski.printhelm.web.controller;

import com.czagrzebski.printhelm.model.ApiCreateJobOrderRequest;
import com.czagrzebski.printhelm.model.ApiJobOrderResponse;
import com.czagrzebski.printhelm.model.ApiUpdateJobOrderRequest;
import com.czagrzebski.printhelm.web.service.JobOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-order")
public class JobOrderController {

    private final JobOrderService jobOrderService;

    public JobOrderController(JobOrderService jobOrderService) {
        this.jobOrderService = jobOrderService;
    }

    @GetMapping
    public ResponseEntity<List<ApiJobOrderResponse>> getJobOrders() {
        return ResponseEntity.ok(jobOrderService.getAllOrders());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiJobOrderResponse> getJobOrderById(@PathVariable long id) {
        return ResponseEntity.ok(jobOrderService.getOrderById(id));
    }

    @PostMapping
    public ResponseEntity<ApiJobOrderResponse> createJobOrder(@RequestBody ApiCreateJobOrderRequest request) {
        return ResponseEntity.status(201).body(jobOrderService.createOrder(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiJobOrderResponse> updateJobOrder(@PathVariable long id, @RequestBody ApiUpdateJobOrderRequest request) {
        return ResponseEntity.ok(jobOrderService.updateOrder(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteJobOrder(@PathVariable long id) {
        jobOrderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}
