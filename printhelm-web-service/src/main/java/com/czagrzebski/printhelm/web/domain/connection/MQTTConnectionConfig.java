package com.czagrzebski.printhelm.web.domain.connection;

import com.czagrzebski.printhelm.model.ConnectionType;
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

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public void setBrokerUrl(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
