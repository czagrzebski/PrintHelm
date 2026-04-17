package com.czagrzebski.printhelm.web.connection;

import com.czagrzebski.printhelm.model.ConnectionType;
import com.czagrzebski.printhelm.model.PrinterType;
import com.czagrzebski.printhelm.web.domain.Printer;
import com.czagrzebski.printhelm.web.domain.connection.MQTTConnectionConfig;
import com.czagrzebski.printhelm.web.event.MqttPrinterMessageReceivedEvent;
import com.czagrzebski.printhelm.web.util.DynamicTrustSSLUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MqttConnectionManager {

    private final Map<Long, MqttClient> mqttClients = new ConcurrentHashMap<>();
    private final Logger logger = LogManager.getLogger(MqttConnectionManager.class);
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public MqttConnectionManager(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void connect(Printer printer) throws MqttException {
        logger.info("Connecting to MQTT broker for printer [ID={}]", printer.getPrinterId());
        if (printer.getConnectionConfig() == null) return;

        if (printer.getConnectionConfig().getConnectionType() != ConnectionType.MQTT) {
            throw new IllegalArgumentException("Printer is not configured for MQTT connection");
        }

        // Check to see if the printer is already connected
        if (mqttClients.containsKey(printer.getPrinterId())) {
            logger.info("Printer [ID={}] is already connected to MQTT broker", printer.getPrinterId());
            return;
        }

        MQTTConnectionConfig mqttConfig = (MQTTConnectionConfig) printer.getConnectionConfig();
        MqttClient mqttClient = new MqttClient(mqttConfig.getBrokerUrl(), mqttConfig.getClientId());
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(mqttConfig.getUsername());
        options.setPassword(mqttConfig.getPassword() != null ? mqttConfig.getPassword().toCharArray() : null);
        options.setAutomaticReconnect(true);
        options.setKeepAliveInterval(60);
        options.setConnectionTimeout(60 * 30); // wait 30 minutes for the connection to be established

        try {
            var uri = new URI(mqttConfig.getBrokerUrl());
            options.setSocketFactory(DynamicTrustSSLUtil.createDynamicTrustSocketFactory(
                    uri.getHost(),
                    uri.getPort()
            ));
            options.setHttpsHostnameVerificationEnabled(false);
        } catch (Exception e) {
            logger.error("Failed to set SSL socket factory for MQTT connection", e);
            throw new RuntimeException(e);
        }

        mqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {
                logger.warn("Connection timed out for printer [ID={}] after 30 minutes. Manual reconnect is required!", printer.getPrinterId());
                disconnect(printer.getPrinterId());
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                String payload = new String(mqttMessage.getPayload());
                logger.info("Message arrived for printer [ID={}]: Topic={}", printer.getPrinterId(), s);
                eventPublisher.publishEvent(new MqttPrinterMessageReceivedEvent(printer.getPrinterId(), payload, s, printer.getPrinterType()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                // No-op
            }
        });

        String stateTopic = null;

        if(printer.getPrinterType() == PrinterType.BAMBULAB) {
            stateTopic = mqttConfig.getTopic() + "/report";
        } else {
            stateTopic = "";
        }

        mqttClient.connect(options);
        mqttClient.subscribe(stateTopic);
        mqttClients.put(printer.getPrinterId(), mqttClient);
    }

    public void publish(Long printerId, String topic, String payload, int qos) throws MqttException {
        MqttClient client = mqttClients.get(printerId);
        if (client == null || !client.isConnected()) {
            throw new IllegalStateException("Printer [ID=" + printerId + "] is not connected");
        }
        MqttMessage message = new MqttMessage(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        message.setQos(qos);
        client.publish(topic, message);
        logger.debug("Published to topic={} for printer [ID={}]", topic, printerId);
    }

    public void disconnect(Long printerId) {
        MqttClient client = mqttClients.remove(printerId);
        if (client != null && client.isConnected()) {
            try {
                client.disconnect();
                client.close();
                logger.info("Disconnected MQTT client for printer [ID={}]", printerId);
            } catch (MqttException e) {
                logger.error("Error disconnecting MQTT client for printer [ID={}]", printerId, e);
            }
        }
    }

    public String generateClientId(String printerName, long printerId) {
        return "printhelm-" + printerName.replaceAll("[^a-zA-Z0-9]", "-").toLowerCase() + "-" + printerId;
    }

}
