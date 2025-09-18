package com.czagrzebski.printhelm.web.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class PrinterStateController {

    @MessageMapping("/printer/state/{printerId}")
    @SendTo("/topic/printer/state/{printerId}")
    public String sendPrinterState(@DestinationVariable String printerId, String message) {
        // Here you can process the incoming message if needed, using printerId
        return message; // Echo the message back to the topic
    }
}
