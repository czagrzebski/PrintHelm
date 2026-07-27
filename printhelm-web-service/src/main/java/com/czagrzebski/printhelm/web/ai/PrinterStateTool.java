package com.czagrzebski.printhelm.web.ai;

import com.czagrzebski.printhelm.model.ApiPrinterState;
import com.czagrzebski.printhelm.web.repository.PrinterRepository;
import com.czagrzebski.printhelm.web.service.PrinterStateCache;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.List;
import java.util.Map;

/**
 * Per-request tool instance; created by {@link ChatToolFactory} for each chat request.
 */
public class PrinterStateTool {

    private final PrinterStateCache printerStateCache;
    private final PrinterRepository printerRepository;
    private final ChatRequestContext ctx;

    public PrinterStateTool(PrinterStateCache printerStateCache,
                            PrinterRepository printerRepository,
                            ChatRequestContext ctx) {
        this.printerStateCache = printerStateCache;
        this.printerRepository = printerRepository;
        this.ctx = ctx;
    }

    @Tool(description = "List all printers with their IDs, names, and models. Call this first to discover available printer IDs.")
    public List<Map<String, Object>> listPrinters() {
        ctx.status("Looking up your printers");
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
        ctx.status("Checking state of " + printerName(printerId));
        return printerStateCache.getState(printerId).orElse(null);
    }

    private String printerName(long printerId) {
        return printerRepository.findById(printerId)
                .map(p -> p.getPrinterName())
                .orElse("printer #" + printerId);
    }
}
