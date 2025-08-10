package com.czagrzebski.printhelm.web.domain.connection;


import com.czagrzebski.printhelm.web.domain.Printer;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="ConnectionConfig")
@DiscriminatorColumn(name="connection_type", discriminatorType = jakarta.persistence.DiscriminatorType.STRING)
public abstract class ConnectionConfig {
    @GeneratedValue
    @Id
    private Long id;

    @OneToOne
    @JoinColumn(name = "printer_id", referencedColumnName = "printer_id")
    private Printer printer;

    public abstract ConnectionType getConnectionType();

    public enum ConnectionType {
        MQTT, CLOUD
    }
}
