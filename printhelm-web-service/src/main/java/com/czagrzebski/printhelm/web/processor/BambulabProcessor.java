package com.czagrzebski.printhelm.web.processor;

import com.czagrzebski.printhelm.model.ApiPrinterState;
import com.czagrzebski.printhelm.web.dto.bambulab.BambulabStateDTO;
import com.czagrzebski.printhelm.web.event.PrinterStateUpdateEvent;
import com.czagrzebski.printhelm.web.mapper.BambuPrinterStateMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class BambulabProcessor {

    private final BambuPrinterStateMapper bambuPrinterStateMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final Logger logger = LogManager.getLogger(BambulabProcessor.class);

    public BambulabProcessor(BambuPrinterStateMapper bambuPrinterStateMapper, ApplicationEventPublisher eventPublisher) {
        this.bambuPrinterStateMapper = bambuPrinterStateMapper;
        this.eventPublisher = eventPublisher;
    }

    public void process(String payload, long printerId) throws JsonProcessingException {
        // check the root json element
        if (payload == null || payload.isEmpty()) {
            throw new IllegalArgumentException("Payload cannot be null or empty");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(payload);

        if (rootNode.has("print") && rootNode.path("print").has("command") && "push_status".equals(rootNode.path("print").path("command").asText())) {
            processPrintMessage(objectMapper, payload, printerId);
        }

    }

    private void processPrintMessage(ObjectMapper mapper, String payload, long printerId) {
        try {
            BambulabStateDTO bambulabState = mapper.readValue(payload, BambulabStateDTO.class);
            ApiPrinterState printerState = bambuPrinterStateMapper.bambuPrinterStateToPrinterState(bambulabState);
            printerState.setTimestamp(java.time.OffsetDateTime.now());
            eventPublisher.publishEvent(new PrinterStateUpdateEvent(printerId, printerState));
        } catch (JsonProcessingException e) {
            // Handle JSON parsing exceptions
            e.printStackTrace();
        }
    }
}
