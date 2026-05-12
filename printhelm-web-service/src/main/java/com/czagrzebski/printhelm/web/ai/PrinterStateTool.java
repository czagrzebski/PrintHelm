package com.czagrzebski.printhelm.web.ai;

import com.czagrzebski.printhelm.model.ApiPrinterState;
import com.czagrzebski.printhelm.web.repository.PrinterRepository;
import com.czagrzebski.printhelm.web.service.PrinterStateCache;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class PrinterStateTool {

    @Autowired
    private PrinterStateCache printerStateCache;

    @Autowired
    private PrinterRepository printerRepository;

    @Tool(description = "List all printers with their IDs, names, and models. Call this first to discover available printer IDs.")
    public List<Map<String, Object>> listPrinters() {
        return printerRepository.findAll().stream()
                .map(p -> Map.<String, Object>of(
                        "id", p.getPrinterId(),
                        "name", p.getPrinterName(),
                        "model", p.getPrinterModel()))
                .toList();
    }

    @Tool(description = "Get the current live state of a specific printer by its numeric ID. Returns temperatures, errors, print progress, remaining time, fan speeds, and more.")
    public ApiPrinterState getPrinterState(
            @ToolParam(description = "Numeric printer ID obtained from listPrinters") long printerId) {
        return printerStateCache.getState(printerId).orElse(null);
    }
}
