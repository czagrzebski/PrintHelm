package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.model.ApiCreatePrinterRequest;
import com.czagrzebski.printhelm.model.ConnectionType;
import com.czagrzebski.printhelm.model.PrinterType;
import com.czagrzebski.printhelm.web.connection.MqttConnectionManager;
import com.czagrzebski.printhelm.web.domain.Printer;
import com.czagrzebski.printhelm.web.domain.bambulab.BambuLabPrinter;
import com.czagrzebski.printhelm.web.domain.connection.ConnectionConfig;
import com.czagrzebski.printhelm.web.domain.connection.MQTTConnectionConfig;
import com.czagrzebski.printhelm.web.mapper.ConnectionConfigurationMapper;
import com.czagrzebski.printhelm.web.mapper.PrinterMapper;
import com.czagrzebski.printhelm.web.repository.ConnectionConfigRepository;
import com.czagrzebski.printhelm.web.repository.PrinterRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

@Service
public class PrinterService {

    private final PrinterRepository printerRepository;
    private final PrinterMapper printerMapper;
    private final MqttConnectionManager mqttConnectionManager;
    private final ConnectionConfigurationMapper connectionConfigurationMapper;

    private final Logger logger = LogManager.getLogger(PrinterService.class);

    public PrinterService(PrinterRepository printerRepository, PrinterMapper printerMapper,
                          MqttConnectionManager mqttConnectionManager, ConnectionConfigurationMapper connectionConfigurationMapper) {
        this.printerRepository = printerRepository;
        this.printerMapper = printerMapper;
        this.mqttConnectionManager = mqttConnectionManager;
        this.connectionConfigurationMapper = connectionConfigurationMapper;
    }

    public Printer createPrinter(ApiCreatePrinterRequest apiCreatePrinterRequest) {
        PrinterType printerType = apiCreatePrinterRequest.getPrinterType();

        if(printerType.equals(PrinterType.BAMBULAB)) {
            BambuLabPrinter printer = (BambuLabPrinter) printerMapper.apiCreatePrinterRequestToBambuLabPrinter( apiCreatePrinterRequest);

            if(apiCreatePrinterRequest.getConnectionConfig() != null && apiCreatePrinterRequest.getConnectionConfig().getConnectionType() == ConnectionType.MQTT) {
                MQTTConnectionConfig connectionConfig = connectionConfigurationMapper.apiConnectionConfigToMqttConnectionConfig(apiCreatePrinterRequest.getConnectionConfig());
                connectionConfig.setClientId(mqttConnectionManager.generateClientId(printer.getPrinterName(), printer.getPrinterId()));
                printer.setConnectionConfig(connectionConfig);
            } else {
                // If no connection config is provided, we can set a default or throw an error
                logger.warn("No MQTT connection configuration provided for BambuLab printer. Defaulting to no connection.");
                printer.setConnectionConfig(null);
            }

            printerRepository.save(printer);
            return printer;
        } else {
            throw new IllegalArgumentException("Unsupported printer type: " + printerType);
        }
    }

    public static String generateClientId(String printerName, long printerId) {
        return "printhelm-" + printerName.replaceAll("[^a-zA-Z0-9]", "-").toLowerCase() + "-" + printerId;
    }


}
