package com.czagrzebski.printhelm.web.mapper;

import com.czagrzebski.printhelm.model.ApiConnectionConfig;
import com.czagrzebski.printhelm.web.domain.connection.MQTTConnectionConfig;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public abstract class ConnectionConfigurationMapper {

    public abstract MQTTConnectionConfig apiConnectionConfigToMqttConnectionConfig(ApiConnectionConfig apiConnectionConfig);

}
