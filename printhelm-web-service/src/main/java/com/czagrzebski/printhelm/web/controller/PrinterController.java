package com.czagrzebski.printhelm.web.controller;

import com.czagrzebski.printhelm.api.PrinterApi;
import com.czagrzebski.printhelm.model.ApiCreatePrinterRequest;
import com.czagrzebski.printhelm.model.ApiPrinterResponse;
import com.czagrzebski.printhelm.model.ApiTempRequest;
import com.czagrzebski.printhelm.model.ApiUpdatePrinterRequest;
import com.czagrzebski.printhelm.model.CreatePrinter201Response;
import com.czagrzebski.printhelm.web.mapper.ConnectionConfigurationMapper;
import com.czagrzebski.printhelm.web.mapper.PrinterMapper;
import com.czagrzebski.printhelm.web.service.PrinterService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping(value="/api")
public class PrinterController implements PrinterApi {

    private final PrinterMapper printerMapper;
    private final ConnectionConfigurationMapper connectionConfigurationMapper;
    private final PrinterService printerService;

    public PrinterController(PrinterMapper printerMapper, PrinterService printerService, ConnectionConfigurationMapper connectionConfigurationMapper) {
        this.printerMapper = printerMapper;
        this.printerService = printerService;
        this.connectionConfigurationMapper = connectionConfigurationMapper;
    }

    @Override
    public ResponseEntity<CreatePrinter201Response> createPrinter(ApiCreatePrinterRequest apiCreatePrinterRequest) {
        var printer = printerService.createPrinter(apiCreatePrinterRequest);
        var response = new CreatePrinter201Response();
        response.setPrinterId(printer.getPrinterId());
        return ResponseEntity.status(201).body(response);
    }

    @Override
    public ResponseEntity<List<ApiPrinterResponse>> getPrinters() {
        return ResponseEntity.ok(printerMapper.printersToApiPrinterResponses(printerService.getAllPrinters()));
    }

    @Override
    public ResponseEntity<ApiPrinterResponse> getPrinterById(Long id) {
        return ResponseEntity.ok(printerMapper.printerToApiPrinterResponse(printerService.getPrinterById(id)));
    }

    @Override
    public ResponseEntity<ApiPrinterResponse> updatePrinter(Long id, ApiUpdatePrinterRequest apiUpdatePrinterRequest) {
        var printer = printerService.updatePrinter(id, apiUpdatePrinterRequest);
        return ResponseEntity.ok(printerMapper.printerToApiPrinterResponse(printer));
    }

    @Override
    public ResponseEntity<Void> deletePrinter(Long id) {
        printerService.deletePrinter(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<Void> setNozzleTemp(Long id, ApiTempRequest apiTempRequest) {
        return ResponseEntity.status(501).build();
    }

    @Override
    public ResponseEntity<Void> setBedTemp(Long id, ApiTempRequest apiTempRequest) {
        return ResponseEntity.status(501).build();
    }
}
