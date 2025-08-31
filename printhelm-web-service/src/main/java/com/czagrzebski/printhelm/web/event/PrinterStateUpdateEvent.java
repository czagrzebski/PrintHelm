package com.czagrzebski.printhelm.web.event;

import com.czagrzebski.printhelm.model.ApiPrinterState;

public class PrinterStateUpdateEvent {
    private final long printerId;
    private ApiPrinterState state;

    public PrinterStateUpdateEvent(long printerId, ApiPrinterState state) {
        this.printerId = printerId;
        this.state = state;
    }

    public long getPrinterId() {
        return printerId;
    }

    public ApiPrinterState getState() {
        return state;
    }

    public void setState(ApiPrinterState state) {
        this.state = state;
    }

}
