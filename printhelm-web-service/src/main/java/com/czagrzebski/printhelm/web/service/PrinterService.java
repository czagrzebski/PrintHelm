package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.model.ApiCreatePrinterRequest;
import com.czagrzebski.printhelm.model.ApiUpdatePrinterRequest;
import java.util.List;
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
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrinterService {

    private final PrinterRepository printerRepository;
    private final PrinterMapper printerMapper;
    private final MqttConnectionManager mqttConnectionManager;
    private final ConnectionConfigurationMapper connectionConfigurationMapper;
    private final AuditLogService auditLogService;

    private final Logger logger = LogManager.getLogger(PrinterService.class);

    public PrinterService(PrinterRepository printerRepository, PrinterMapper printerMapper,
                          MqttConnectionManager mqttConnectionManager, ConnectionConfigurationMapper connectionConfigurationMapper,
                          AuditLogService auditLogService) {
        this.printerRepository = printerRepository;
        this.printerMapper = printerMapper;
        this.mqttConnectionManager = mqttConnectionManager;
        this.connectionConfigurationMapper = connectionConfigurationMapper;
        this.auditLogService = auditLogService;
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
                logger.warn("No MQTT connection configuration provided for BambuLab printer. Defaulting to no connection.");
                printer.setConnectionConfig(null);
            }

            printerRepository.save(printer);
            auditLogService.record("PRINTER_CREATED", "Printer", printer.getPrinterId(),
                    printer.getPrinterName() + " (" + printer.getPrinterModel() + ")");
            return printer;
        } else {
            throw new IllegalArgumentException("Unsupported printer type: " + printerType);
        }
    }

    @Transactional
    public Printer updatePrinter(long id, ApiUpdatePrinterRequest request) {
        Printer printer = getPrinterById(id);

        if (request.getPrinterName() != null) printer.setPrinterName(request.getPrinterName());
        if (request.getPrinterModel() != null) printer.setPrinterModel(request.getPrinterModel());
        printer.setLocation(request.getLocation());
        printer.setSerialNumber(request.getSerialNumber());

        if (request.getConnectionConfig() != null) {
            mqttConnectionManager.disconnect(id);
            MQTTConnectionConfig newConfig = connectionConfigurationMapper.apiConnectionConfigToMqttConnectionConfig(request.getConnectionConfig());
            newConfig.setClientId(mqttConnectionManager.generateClientId(printer.getPrinterName(), id));
            printer.setConnectionConfig(newConfig);
            printerRepository.save(printer);
            try {
                mqttConnectionManager.connect(printer);
            } catch (Exception e) {
                logger.warn("Failed to reconnect printer [ID={}] after update: {}", id, e.getMessage());
            }
        } else {
            printerRepository.save(printer);
        }

        auditLogService.record("PRINTER_UPDATED", "Printer", id, printer.getPrinterName());
        return printer;
    }

    @Transactional
    public void deletePrinter(long id) {
        Printer printer = getPrinterById(id);
        mqttConnectionManager.disconnect(id);
        printerRepository.delete(printer);
        auditLogService.record("PRINTER_DELETED", "Printer", id, printer.getPrinterName());
    }

    public List<Printer> getAllPrinters() {
        return printerRepository.findAll();
    }

    public Printer getPrinterById(long id) {
        return printerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Printer not found: " + id));
    }

    public static String generateClientId(String printerName, long printerId) {
        return "printhelm-" + printerName.replaceAll("[^a-zA-Z0-9]", "-").toLowerCase() + "-" + printerId;
    }
}
