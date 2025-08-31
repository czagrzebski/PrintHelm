package com.czagrzebski.printhelm.web.domain.connection;


import com.czagrzebski.printhelm.model.ConnectionType;
import com.czagrzebski.printhelm.web.domain.Printer;
import jakarta.persistence.Column;
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

    @OneToOne(mappedBy = "connectionConfig")
    private Printer printer;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Printer getPrinter() {
        return printer;
    }

    public void setPrinter(Printer printer) {
        this.printer = printer;
    }

    public ConnectionType getConnectionType() {
        return ConnectionType.valueOf(
                this.getClass()
                        .getAnnotation(jakarta.persistence.DiscriminatorValue.class)
                        .value()
        );
    }

    public void setConnectionType(ConnectionType connectionType) {
        // No-op, required by JPA
    }
}
