package com.czagrzebski.printhelm.web.event;

import com.czagrzebski.printhelm.model.PrinterType;

public class MqttPrinterMessageReceivedEvent {

    private final long printerId;
    private final String message;
    private final String topic;
    private final PrinterType printerType;

    public MqttPrinterMessageReceivedEvent(long printerId, String message, String topic, PrinterType printerType) {
        this.printerId = printerId;
        this.message = message;
        this.topic = topic;
        this.printerType = printerType;
    }

    public long getPrinterId() {
        return printerId;
    }

    public String getMessage() {
        return message;
    }

    public String getTopic() {
        return topic;
    }

    public PrinterType getPrinterType() {
        return printerType;
    }
}
