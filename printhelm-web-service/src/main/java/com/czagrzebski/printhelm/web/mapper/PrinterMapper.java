package com.czagrzebski.printhelm.web.mapper;

import com.czagrzebski.printhelm.model.ApiConnectionConfig;
import com.czagrzebski.printhelm.model.ApiCreatePrinterRequest;
import com.czagrzebski.printhelm.model.ApiPrinterResponse;
import com.czagrzebski.printhelm.model.ConnectionType;
import com.czagrzebski.printhelm.web.domain.Printer;
import com.czagrzebski.printhelm.web.domain.bambulab.BambuLabPrinter;
import com.czagrzebski.printhelm.web.domain.connection.MQTTConnectionConfig;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE, uses = {ConnectionConfigurationMapper.class})
public abstract class PrinterMapper {

    @Mapping(target = "connectionConfig", source = "apiCreatePrinterRequest.connectionConfig")
    public abstract BambuLabPrinter apiCreatePrinterRequestToBambuLabPrinter(ApiCreatePrinterRequest apiCreatePrinterRequest);

    @Mapping(target = "connectionConfig", ignore = true)
    public abstract ApiPrinterResponse printerToApiPrinterResponse(Printer printer);

    public abstract List<ApiPrinterResponse> printersToApiPrinterResponses(List<Printer> printers);

    @AfterMapping
    protected void mapConnectionConfig(Printer printer, @MappingTarget ApiPrinterResponse response) {
        if (printer.getConnectionConfig() instanceof MQTTConnectionConfig mqttConfig) {
            ApiConnectionConfig config = new ApiConnectionConfig();
            config.setConnectionType(ConnectionType.MQTT);
            config.setBrokerUrl(mqttConfig.getBrokerUrl());
            config.setTopic(mqttConfig.getTopic());
            config.setUsername(mqttConfig.getUsername());
            config.setPassword(mqttConfig.getPassword());
            response.setConnectionConfig(config);
        }
    }
}
