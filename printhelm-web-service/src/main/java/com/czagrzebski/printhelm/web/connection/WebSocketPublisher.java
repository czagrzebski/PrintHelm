package com.czagrzebski.printhelm.web.connection;

import com.czagrzebski.printhelm.web.event.PrinterStateUpdateEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handlePrinterStateUpdateEvent(PrinterStateUpdateEvent event) {
        String destination = "/topic/printer/" + event.getPrinterId() + "/state";
        messagingTemplate.convertAndSend(destination, event.getState());
    }

}
