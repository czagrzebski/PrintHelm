package com.czagrzebski.printhelm.web.listener;

import com.czagrzebski.printhelm.model.ApiPrinterState;
import com.czagrzebski.printhelm.web.domain.notification.NotificationSeverity;
import com.czagrzebski.printhelm.web.domain.notification.NotificationType;
import com.czagrzebski.printhelm.web.event.PrinterStateUpdateEvent;
import com.czagrzebski.printhelm.web.service.NotificationService;
import com.czagrzebski.printhelm.web.service.PrinterService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class PrinterNotificationListener {

    private static final Logger logger = LogManager.getLogger(PrinterNotificationListener.class);

    private final NotificationService notificationService;
    private final PrinterService printerService;

    // keyed by printerId
    private final ConcurrentHashMap<Long, String> previousGcodeState = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> previousStageState = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Integer> previousPrintError = new ConcurrentHashMap<>();

    private static final java.util.Set<String> IDLE_STAGES = java.util.Set.of("idle", "offline");

    public PrinterNotificationListener(NotificationService notificationService, PrinterService printerService) {
        this.notificationService = notificationService;
        this.printerService = printerService;
    }

    @EventListener
    public void onPrinterStateUpdate(PrinterStateUpdateEvent event) {
        ApiPrinterState state = event.getState();
        long printerId = event.getPrinterId();

        String currentGcodeState = state.getGcodeState();

        // Only process when gcodeState is present
        if (currentGcodeState == null || currentGcodeState.isBlank()) {
            return;
        }

        String prevGcodeState = previousGcodeState.get(printerId);
        String currentStageState = state.getState();
        String prevStageState = previousStageState.get(printerId);

        String printerName = resolvePrinterName(printerId);
        String file = state.getFile();

        // Detect job submission: stgCur leaves idle/offline → printer entered prep sequence
        if (currentStageState != null && !currentStageState.equalsIgnoreCase(prevStageState)) {
            boolean wasIdle = prevStageState == null || IDLE_STAGES.contains(prevStageState.toLowerCase());
            boolean nowActive = !IDLE_STAGES.contains(currentStageState.toLowerCase());
            if (wasIdle && nowActive) {
                String fileLabel = (file != null && !file.isBlank()) ? " – " + file : "";
                notificationService.createNotification(
                        printerId, printerName,
                        NotificationType.PRINT_JOB_STARTED, NotificationSeverity.INFO,
                        "Print Job Started on " + printerName,
                        "A print job has been submitted" + fileLabel + ".", file);
            }
            previousStageState.put(printerId, currentStageState);
        }

        if (!currentGcodeState.equalsIgnoreCase(prevGcodeState)) {
            handleStateTransition(printerId, printerName, prevGcodeState, currentGcodeState, file, state);
            previousGcodeState.put(printerId, currentGcodeState);
        }

        // Only process error tracking when the field is present in this message.
        // Partial push_status messages omit printError entirely (null), which must not
        // reset the last-seen code and cause duplicate notifications.
        if (state.getPrintError() != null) {
            int currentError = state.getPrintError();
            int prevError = previousPrintError.getOrDefault(printerId, 0);

            if (currentError != 0 && currentError != prevError) {
                notificationService.createNotification(
                        printerId, printerName,
                        NotificationType.PRINT_ERROR, NotificationSeverity.ERROR,
                        "Print Error on " + printerName,
                        buildErrorMessage(state), file);
            }
            previousPrintError.put(printerId, currentError);
        }
    }

    private void handleStateTransition(long printerId, String printerName,
                                        String prevState, String currentState,
                                        String file, ApiPrinterState state) {
        String fileLabel = (file != null && !file.isBlank()) ? " – " + file : "";

        switch (currentState.toUpperCase()) {
            case "RUNNING" -> {
                if ("PAUSE".equalsIgnoreCase(prevState)) {
                    notificationService.createNotification(
                            printerId, printerName,
                            NotificationType.PRINT_RESUMED, NotificationSeverity.INFO,
                            "Print Resumed on " + printerName,
                            "The print job has been resumed" + fileLabel + ".", file);
                } else {
                    // covers null, IDLE, FINISH, FAILED, PREPARE, and any other prep/intermediate state
                    notificationService.createNotification(
                            printerId, printerName,
                            NotificationType.PRINT_STARTED, NotificationSeverity.INFO,
                            "Began Printing on " + printerName,
                            "GCode is now executing" + fileLabel + ".", file);
                }
            }
            case "PAUSE" -> notificationService.createNotification(
                    printerId, printerName,
                    NotificationType.PRINT_PAUSED, NotificationSeverity.WARNING,
                    "Print Paused on " + printerName,
                    "The print job has been paused" + fileLabel + ".", file);

            case "FINISH" -> notificationService.createNotification(
                    printerId, printerName,
                    NotificationType.PRINT_COMPLETED, NotificationSeverity.INFO,
                    "Print Completed on " + printerName,
                    "The print job completed successfully" + fileLabel + ".", file);

            case "FAILED" -> {
                String suffix = state.getFailReason() != null ? " Reason: " + state.getFailReason() : "";
                notificationService.createNotification(
                        printerId, printerName,
                        NotificationType.PRINT_FAILED, NotificationSeverity.ERROR,
                        "Print Failed on " + printerName,
                        "The print job has failed" + fileLabel + "." + suffix, file);
            }
            case "IDLE" -> {
                // IDLE after an active job = stopped by user command
                if ("RUNNING".equalsIgnoreCase(prevState) || "PAUSE".equalsIgnoreCase(prevState)) {
                    notificationService.createNotification(
                            printerId, printerName,
                            NotificationType.PRINT_STOPPED, NotificationSeverity.WARNING,
                            "Print Stopped on " + printerName,
                            "The print job was stopped" + fileLabel + ".", file);
                }
            }
            default -> logger.debug("Unhandled gcodeState transition {} → {} for printer {}", prevState, currentState, printerId);
        }
    }

    private String buildErrorMessage(ApiPrinterState state) {
        StringBuilder sb = new StringBuilder("A print error has occurred.");
        if (state.getMcPrintErrorCode() != null) {
            sb.append(" Error code: ").append(state.getMcPrintErrorCode()).append(".");
        }
        if (state.getFailReason() != null) {
            sb.append(" Reason: ").append(state.getFailReason()).append(".");
        }
        return sb.toString();
    }

    private String resolvePrinterName(long printerId) {
        try {
            return printerService.getPrinterById(printerId).getPrinterName();
        } catch (Exception e) {
            return "Printer #" + printerId;
        }
    }
}
