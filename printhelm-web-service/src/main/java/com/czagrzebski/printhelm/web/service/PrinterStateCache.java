package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.model.ApiPrinterState;
import com.czagrzebski.printhelm.web.event.PrinterStateUpdateEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PrinterStateCache {

    private final ConcurrentHashMap<Long, ApiPrinterState> states = new ConcurrentHashMap<>();

    @EventListener
    public void onStateUpdate(PrinterStateUpdateEvent event) {
        states.put(event.getPrinterId(), event.getState());
    }

    public Optional<ApiPrinterState> getState(long printerId) {
        return Optional.ofNullable(states.get(printerId));
    }
}
