package com.czagrzebski.printhelm.web.listener;

import com.czagrzebski.printhelm.web.event.MqttPrinterMessageReceivedEvent;
import com.czagrzebski.printhelm.web.processor.BambulabProcessor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MqttEventListener {

    private BambulabProcessor bambulabProcessor;

    public MqttEventListener(BambulabProcessor bambulabProcessor) {
        this.bambulabProcessor = bambulabProcessor;
    }

    @EventListener
    public void handleMqttPrinterMessageReceivedEvent(MqttPrinterMessageReceivedEvent event) {
        if(event.getPrinterType() == null) {
            return; // or throw an exception if printer type is mandatory
        }

        switch (event.getPrinterType()) {
            case BAMBULAB:
                try {
                    bambulabProcessor.process(event.getMessage(), event.getPrinterId());
                } catch (Exception e) {
                    // Handle exception, log error, etc.
                    e.printStackTrace();
                }
                break;
            default:
                // Handle other printer types if necessary
                break;
        }
    }

}
