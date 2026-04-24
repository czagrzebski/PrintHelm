package com.czagrzebski.printhelm.web.controller;

import com.czagrzebski.printhelm.model.ApiCreatePrinterRequest;
import com.czagrzebski.printhelm.model.ApiPrinterResponse;
import com.czagrzebski.printhelm.model.ApiUpdatePrinterRequest;
import com.czagrzebski.printhelm.model.CreatePrinter201Response;
import com.czagrzebski.printhelm.web.mapper.ConnectionConfigurationMapper;
import com.czagrzebski.printhelm.web.mapper.PrinterMapper;
import com.czagrzebski.printhelm.web.service.PrinterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/printer")
public class PrinterController {

    private final PrinterMapper printerMapper;
    private final ConnectionConfigurationMapper connectionConfigurationMapper;
    private final PrinterService printerService;

    public PrinterController(PrinterMapper printerMapper, PrinterService printerService, ConnectionConfigurationMapper connectionConfigurationMapper) {
        this.printerMapper = printerMapper;
        this.printerService = printerService;
        this.connectionConfigurationMapper = connectionConfigurationMapper;
    }

    @PostMapping("/createPrinter")
    public ResponseEntity<CreatePrinter201Response> createPrinter(@RequestBody ApiCreatePrinterRequest request) {
        var printer = printerService.createPrinter(request);
        var response = new CreatePrinter201Response();
        response.setPrinterId(printer.getPrinterId());
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ApiPrinterResponse>> getPrinters() {
        return ResponseEntity.ok(printerMapper.printersToApiPrinterResponses(printerService.getAllPrinters()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiPrinterResponse> getPrinterById(@PathVariable long id) {
        return ResponseEntity.ok(printerMapper.printerToApiPrinterResponse(printerService.getPrinterById(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiPrinterResponse> updatePrinter(@PathVariable long id, @RequestBody ApiUpdatePrinterRequest request) {
        var printer = printerService.updatePrinter(id, request);
        return ResponseEntity.ok(printerMapper.printerToApiPrinterResponse(printer));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePrinter(@PathVariable long id) {
        printerService.deletePrinter(id);
        return ResponseEntity.noContent().build();
    }
}
