package com.czagrzebski.printhelm.web.domain.connection;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("MQTT")
public class MQTTConnectionConfig extends ConnectionConfig {

    @Column(name="broker_url", length=255, nullable=false, unique=false)
    private String brokerUrl;

    @Column(name="client_id", length=255, nullable=false, unique=false)
    private String clientId;

    @Column(name="username", length=255, nullable=true, unique=false)
    private String username;

    @Column(name="password", length=255, nullable=true, unique=false)
    private String password;

    @Column(name="topic", length=255, nullable=false, unique=false)
    private String topic;

    @Override
    public ConnectionType getConnectionType() {
        return ConnectionType.MQTT;
    }
}
