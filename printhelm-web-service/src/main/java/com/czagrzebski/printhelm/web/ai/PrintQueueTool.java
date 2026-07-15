package com.czagrzebski.printhelm.web.ai;

import com.czagrzebski.printhelm.model.ApiJobOrderResponse;
import com.czagrzebski.printhelm.model.ApiProposedAction;
import com.czagrzebski.printhelm.model.ApiProposedAction.ActionTypeEnum;
import com.czagrzebski.printhelm.web.repository.PrinterRepository;
import com.czagrzebski.printhelm.web.service.AuditLogService;
import com.czagrzebski.printhelm.web.service.PrintQueueService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Per-request tool instance; created by {@link ChatToolFactory} for each chat request.
 */
public class PrintQueueTool {

    private final PrintQueueService printQueueService;
    private final PrinterRepository printerRepository;
    private final ChatRequestContext ctx;
    private final AuditLogService auditLogService;

    public PrintQueueTool(PrintQueueService printQueueService,
                          PrinterRepository printerRepository,
                          ChatRequestContext ctx,
                          AuditLogService auditLogService) {
        this.printQueueService = printQueueService;
        this.printerRepository = printerRepository;
        this.ctx = ctx;
        this.auditLogService = auditLogService;
    }

    @Tool(description = """
            Get the print queue for a specific printer. Returns queued jobs in order with job IDs, filenames, customer names, status, and gcode filament metadata (type and hex color per slot) needed for AMS mapping.
            Only jobs with status READY_TO_PRINT can be started. Jobs with status PRINTING are already running; PRINT_FINISHED jobs are done.
            """)
    public List<ApiJobOrderResponse> getPrinterQueue(
            @ToolParam(description = "Numeric printer ID obtained from listPrinters") long printerId) {
        ctx.status("Reading the print queue");
        return printQueueService.getQueue(printerId);
    }

    @Tool(description = """
            Propose starting a queued print job on a specific printer. The action is queued for user approval and will NOT execute until the user confirms.
            Only call this for jobs with status READY_TO_PRINT.
            Before calling: (1) call getPrinterQueue to find a READY_TO_PRINT job and get its jobOrderId and filament metadata, \
            (2) call getPrinterState to see AMS trays (materialSystem.materials) with their type and color.
            amsMapping rules: \
            - If getPrinterState returned AMS trays: amsMapping[i] = AMS tray index (0-based) for filament slot i, matched by filament type. \
            - If getPrinterState returned no AMS trays or returned null: pass an empty array []. \
            - If the job has no filament metadata (plain .gcode, no gcodeMetadata): pass an empty array [].
            """)
    public String proposeStartPrint(
            @ToolParam(description = "Numeric printer ID") long printerId,
            @ToolParam(description = "Job order ID from getPrinterQueue") long jobOrderId,
            @ToolParam(description = "AMS tray index per filament slot; amsMapping[i] = tray index for slot i") List<Integer> amsMapping) {

        String printerName = printerRepository.findById(printerId)
                .map(p -> p.getPrinterName())
                .orElse("printer #" + printerId);

        ApiProposedAction action = new ApiProposedAction();
        action.setActionId(UUID.randomUUID().toString());
        action.setActionType(ActionTypeEnum.START_PRINT);
        action.setPrinterId(printerId);
        action.setPrinterName(printerName);
        action.setDescription("Start print job #" + jobOrderId + " on " + printerName);

        Map<String, Object> params = new HashMap<>();
        params.put("jobOrderId", jobOrderId);
        params.put("amsMapping", amsMapping != null ? amsMapping : List.of());
        params.put("flowCali", true);
        params.put("vibrationCali", true);
        params.put("layerInspect", true);
        action.setParameters(params);

        ctx.status("Preparing action: " + action.getDescription());
        ctx.addAction(action);
        auditLogService.record("AI_ACTION_PROPOSED", "Printer", printerId,
                "AI assistant proposed: " + action.getDescription());

        return "Action proposed: \"" + action.getDescription() + "\". Awaiting explicit user confirmation — this will NOT execute automatically.";
    }
}
