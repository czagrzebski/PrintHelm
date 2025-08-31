package com.czagrzebski.printhelm.web.controller;

import com.czagrzebski.printhelm.api.PrinterApi;
import com.czagrzebski.printhelm.model.ApiCreatePrinterRequest;
import com.czagrzebski.printhelm.model.CreatePrinter201Response;
import com.czagrzebski.printhelm.web.mapper.ConnectionConfigurationMapper;
import com.czagrzebski.printhelm.web.mapper.PrinterMapper;
import com.czagrzebski.printhelm.web.service.PrinterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

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
}
