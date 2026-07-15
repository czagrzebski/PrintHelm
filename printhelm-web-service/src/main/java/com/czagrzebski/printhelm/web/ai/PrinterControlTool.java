package com.czagrzebski.printhelm.web.ai;

import com.czagrzebski.printhelm.model.ApiProposedAction;
import com.czagrzebski.printhelm.model.ApiProposedAction.ActionTypeEnum;
import com.czagrzebski.printhelm.web.repository.PrinterRepository;
import com.czagrzebski.printhelm.web.service.AuditLogService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Map;
import java.util.UUID;

/**
 * Per-request tool instance; created by {@link ChatToolFactory} for each chat request.
 * Proposed actions accumulate on the {@link ChatRequestContext}.
 */
public class PrinterControlTool {

    private final PrinterRepository printerRepository;
    private final ChatRequestContext ctx;
    private final AuditLogService auditLogService;

    public PrinterControlTool(PrinterRepository printerRepository, ChatRequestContext ctx,
                              AuditLogService auditLogService) {
        this.printerRepository = printerRepository;
        this.ctx = ctx;
        this.auditLogService = auditLogService;
    }

    @Tool(description = "Propose pausing the current print job on a specific printer. The action is queued for user approval and will NOT execute until the user confirms.")
    public String proposePause(@ToolParam(description = "Numeric printer ID") long printerId) {
        return propose(ActionTypeEnum.PAUSE, printerId, "Pause print on", Map.of());
    }

    @Tool(description = "Propose resuming a paused print job on a specific printer. The action is queued for user approval and will NOT execute until the user confirms.")
    public String proposeResume(@ToolParam(description = "Numeric printer ID") long printerId) {
        return propose(ActionTypeEnum.RESUME, printerId, "Resume print on", Map.of());
    }

    @Tool(description = "Propose stopping (cancelling) the current print job on a specific printer. The action is queued for user approval and will NOT execute until the user confirms.")
    public String proposeStop(@ToolParam(description = "Numeric printer ID") long printerId) {
        return propose(ActionTypeEnum.STOP, printerId, "Stop print on", Map.of());
    }

    @Tool(description = "Propose setting the nozzle target temperature on a specific printer. The action is queued for user approval and will NOT execute until the user confirms. Use 0 to turn off heating.")
    public String proposeSetNozzleTemp(
            @ToolParam(description = "Numeric printer ID") long printerId,
            @ToolParam(description = "Target nozzle temperature in °C (0–300). Use 0 to turn off heating.") int temp) {
        String description = temp == 0 ? "Turn off nozzle heating on" : "Set nozzle to " + temp + "°C on";
        return propose(ActionTypeEnum.SET_NOZZLE_TEMP, printerId, description, Map.of("temp", temp));
    }

    @Tool(description = "Propose setting the bed target temperature on a specific printer. The action is queued for user approval and will NOT execute until the user confirms. Use 0 to turn off heating.")
    public String proposeSetBedTemp(
            @ToolParam(description = "Numeric printer ID") long printerId,
            @ToolParam(description = "Target bed temperature in °C (0–110). Use 0 to turn off heating.") int temp) {
        String description = temp == 0 ? "Turn off bed heating on" : "Set bed to " + temp + "°C on";
        return propose(ActionTypeEnum.SET_BED_TEMP, printerId, description, Map.of("temp", temp));
    }

    @Tool(description = "Propose changing the print speed level on a specific printer. The action is queued for user approval and will NOT execute until the user confirms. Speed levels: 1=Silent, 2=Standard, 3=Sport, 4=Ludicrous.")
    public String proposeSetSpeed(
            @ToolParam(description = "Numeric printer ID") long printerId,
            @ToolParam(description = "Speed level: 1=Silent, 2=Standard, 3=Sport, 4=Ludicrous") int speedLevel) {
        String[] labels = {"", "Silent", "Standard", "Sport", "Ludicrous"};
        String label = (speedLevel >= 1 && speedLevel <= 4) ? labels[speedLevel] : String.valueOf(speedLevel);
        return propose(ActionTypeEnum.SET_SPEED, printerId, "Set speed to " + label + " on", Map.of("speed", speedLevel));
    }

    @Tool(description = "Propose homing all axes on a specific printer. The action is queued for user approval and will NOT execute until the user confirms.")
    public String proposeHome(@ToolParam(description = "Numeric printer ID") long printerId) {
        return propose(ActionTypeEnum.HOME, printerId, "Home all axes on", Map.of());
    }

    private String propose(ActionTypeEnum type, long printerId, String descriptionPrefix, Map<String, Object> parameters) {
        String printerName = printerRepository.findById(printerId)
                .map(p -> p.getPrinterName())
                .orElse("printer #" + printerId);

        ApiProposedAction action = new ApiProposedAction();
        action.setActionId(UUID.randomUUID().toString());
        action.setActionType(type);
        action.setPrinterId(printerId);
        action.setPrinterName(printerName);
        action.setDescription(descriptionPrefix + " " + printerName);
        action.setParameters(parameters);

        ctx.status("Preparing action: " + action.getDescription());
        ctx.addAction(action);
        auditLogService.record("AI_ACTION_PROPOSED", "Printer", printerId,
                "AI assistant proposed: " + action.getDescription());

        return "Action proposed: \"" + action.getDescription() + "\". Awaiting explicit user confirmation — this will NOT execute automatically.";
    }
}
