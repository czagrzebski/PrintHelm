package com.czagrzebski.printhelm.web.ai;

import com.czagrzebski.printhelm.model.ApiDiagnosticReport;
import com.czagrzebski.printhelm.model.ApiPrinterState;
import com.czagrzebski.printhelm.web.domain.Printer;
import com.czagrzebski.printhelm.web.domain.notification.PrinterNotification;
import com.czagrzebski.printhelm.web.repository.PrinterNotificationRepository;
import com.czagrzebski.printhelm.web.repository.PrinterRepository;
import com.czagrzebski.printhelm.web.service.PrinterStateCache;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PrinterDiagnosticService {

    private static final String SYSTEM_PROMPT = """
            You are a 3D printer diagnostics expert. You will receive the current state of a Bambu Lab
            printer with all error codes already decoded to human-readable descriptions — you do not
            need to recall or interpret any raw codes yourself.

            Analyze the provided data and return a JSON diagnostic report. Focus on reasoning about
            root causes and practical troubleshooting steps based on the decoded information provided.

            Consider temperature deviations, HMS error descriptions, fan speeds, print progress,
            recent notification history, and the current printer stage when forming your analysis.

            Mark the printer as healthy (healthy=true, severity=NONE) only when no errors or
            anomalies are present. If there are active HMS errors or a non-zero printError, always
            report the issue.

            Return a JSON object with exactly these fields:
            - healthy (boolean)
            - issue (string or null): one-line summary of the problem
            - likelyCause (string or null): the most probable root cause
            - severity (string): one of NONE, LOW, MEDIUM, HIGH, CRITICAL
            - troubleshootingSteps (array of strings): ordered steps to fix the issue, empty if healthy
            - watchFor (string or null): what to monitor after applying fixes
            """;

    private final ChatClient chatClient;
    private final PrinterStateCache printerStateCache;
    private final PrinterNotificationRepository notificationRepository;
    private final PrinterRepository printerRepository;

    public PrinterDiagnosticService(ChatClient.Builder builder,
                                    PrinterStateCache printerStateCache,
                                    PrinterNotificationRepository notificationRepository,
                                    PrinterRepository printerRepository) {
        this.printerStateCache = printerStateCache;
        this.notificationRepository = notificationRepository;
        this.printerRepository = printerRepository;
        this.chatClient = builder.build();
    }

    public ApiDiagnosticReport diagnose(long printerId) {
        Printer printer = printerRepository.findById(printerId)
                .orElseThrow(() -> new IllegalArgumentException("Printer not found: " + printerId));

        Optional<ApiPrinterState> stateOpt = printerStateCache.getState(printerId);
        List<PrinterNotification> recentNotifications = notificationRepository
                .findByPrinterIdOrderByCreatedAtDesc(printerId)
                .stream()
                .limit(20)
                .collect(Collectors.toList());

        String userPrompt = buildDiagnosticPrompt(printer, stateOpt.orElse(null), recentNotifications);

        ApiDiagnosticReport result = chatClient.prompt()
                .messages(
                        new SystemMessage(SYSTEM_PROMPT),
                        new UserMessage(userPrompt)
                )
                .options(AnthropicChatOptions.builder()
                        .model("claude-haiku-4-5-20251001")
                        .build())
                .call()
                .entity(ApiDiagnosticReport.class);

        return result != null ? result : healthyReport();
    }

    private String buildDiagnosticPrompt(Printer printer, ApiPrinterState state, List<PrinterNotification> notifications) {
        StringBuilder sb = new StringBuilder();
        sb.append("Diagnose the following Bambu Lab printer:\n\n");
        sb.append("Printer: ").append(printer.getPrinterName())
          .append(" (").append(printer.getPrinterModel()).append(")\n\n");

        if (state == null) {
            sb.append("CURRENT STATE: No live telemetry available (printer may be offline).\n\n");
        } else {
            sb.append("CURRENT STATE:\n");
            sb.append("  Status: ").append(state.getState()).append("\n");
            sb.append("  GCode state: ").append(state.getGcodeState()).append("\n");

            if (state.getPrintErrorDescription() != null) {
                sb.append("  Print error: ").append(state.getPrintErrorDescription()).append("\n");
            } else if (state.getPrintError() != null && state.getPrintError() != 0) {
                sb.append("  Print error (unrecognized code): 0x")
                  .append(Integer.toHexString(state.getPrintError()).toUpperCase()).append("\n");
            }

            if (state.getMcPrintErrorCode() != null) {
                sb.append("  MC error code: ").append(state.getMcPrintErrorCode()).append("\n");
            }
            if (state.getFailReason() != null) {
                sb.append("  Fail reason: ").append(state.getFailReason()).append("\n");
            }

            if (state.getHmsErrors() != null && !state.getHmsErrors().isEmpty()) {
                sb.append("  HMS hardware errors:\n");
                state.getHmsErrors().forEach(e -> sb.append("    - ").append(e).append("\n"));
            }

            sb.append("  Nozzle temp: ").append(state.getNozzleTemp()).append("°C")
              .append(" (target: ").append(state.getNozzleTargetTemp()).append("°C)\n");
            sb.append("  Bed temp: ").append(state.getBedTemp()).append("°C")
              .append(" (target: ").append(state.getBedTargetTemp()).append("°C)\n");
            sb.append("  Progress: ").append(state.getProgress()).append("%\n");
            sb.append("  Layer: ").append(state.getCurrentLayer())
              .append(" / ").append(state.getTotalLayers()).append("\n");
            sb.append("  Remaining: ").append(state.getRemainTime()).append(" min\n");
            sb.append("  Speed: level ").append(state.getSpdLvl())
              .append(" (").append(state.getSpdMag()).append("%)\n");

            if (state.getFans() != null && !state.getFans().isEmpty()) {
                sb.append("  Fans:\n");
                state.getFans().forEach(f ->
                    sb.append("    - ").append(f.getName()).append(": ").append(f.getSpeed()).append("\n"));
            }

            if (state.getMaterialSystem() != null && state.getMaterialSystem().getMaterials() != null
                    && !state.getMaterialSystem().getMaterials().isEmpty()) {
                sb.append("  AMS materials:\n");
                state.getMaterialSystem().getMaterials().forEach(m ->
                    sb.append("    - ").append(m.getType())
                      .append(m.getLoaded() != null && m.getLoaded() ? " [loaded]" : "").append("\n"));
            }
        }

        if (!notifications.isEmpty()) {
            sb.append("\nRECENT NOTIFICATIONS (newest first):\n");
            notifications.forEach(n ->
                sb.append("  [").append(n.getSeverity()).append("] ")
                  .append(n.getType()).append(": ").append(n.getTitle())
                  .append(n.getMessage() != null ? " — " + n.getMessage() : "")
                  .append(" (").append(n.getCreatedAt()).append(")\n"));
        }

        return sb.toString();
    }

    private ApiDiagnosticReport healthyReport() {
        ApiDiagnosticReport report = new ApiDiagnosticReport();
        report.setHealthy(true);
        report.setSeverity(ApiDiagnosticReport.SeverityEnum.NONE);
        report.setTroubleshootingSteps(List.of());
        return report;
    }
}
