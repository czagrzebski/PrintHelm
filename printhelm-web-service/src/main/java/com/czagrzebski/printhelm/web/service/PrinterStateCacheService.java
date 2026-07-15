package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.model.ApiPrinterState;
import com.czagrzebski.printhelm.web.event.PrinterStateUpdateEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Keeps the most recent state received from each printer over MQTT so clients
 * can read it immediately instead of waiting for the next WebSocket push.
 */
@Service
public class PrinterStateCacheService {

    private final Map<Long, ApiPrinterState> lastStates = new ConcurrentHashMap<>();

    @EventListener
    public void handlePrinterStateUpdateEvent(PrinterStateUpdateEvent event) {
        lastStates.put(event.getPrinterId(), event.getState());
    }

    public Optional<ApiPrinterState> getState(long printerId) {
        return Optional.ofNullable(lastStates.get(printerId));
    }

    public Map<String, ApiPrinterState> getAllStates() {
        return lastStates.entrySet().stream()
                .collect(Collectors.toMap(e -> String.valueOf(e.getKey()), Map.Entry::getValue));
    }
}
