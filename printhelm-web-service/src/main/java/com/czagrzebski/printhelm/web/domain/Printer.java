package com.czagrzebski.printhelm.web.domain;

import com.czagrzebski.printhelm.model.PrinterType;
import com.czagrzebski.printhelm.web.domain.connection.ConnectionConfig;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="Printer")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "printer_type", discriminatorType = DiscriminatorType.STRING)
public class Printer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="printer_id")
    private long printerId;

    @Column(name="printer_name", length=50, nullable=false)
    private String printerName;

    @Column(name="printer_model", length=50, nullable=false)
    private String printerModel;

    @Column(name="location", length=50)
    private String location;

    @Column(name="serial_number", length=50)
    private String serialNumber;

    @Column(name="auto_connect_on_startup", nullable=false)
    private boolean autoConnectOnStartup = true;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "connection_config_id") // Foreign key column
    private ConnectionConfig connectionConfig;

    public long getPrinterId() {
        return printerId;
    }

    public void setPrinterId(long printerId) {
        this.printerId = printerId;
    }

    public String getPrinterName() {
        return printerName;
    }

    public void setPrinterName(String printerName) {
        this.printerName = printerName;
    }

    @jakarta.persistence.Transient
    public PrinterType getPrinterType() {
        return PrinterType.valueOf(
                this.getClass()
                        .getAnnotation(jakarta.persistence.DiscriminatorValue.class)
                        .value()
        );
    }

    @jakarta.persistence.Transient
    public void setPrinterType(PrinterType printerType) {
        // No-op: printerType is determined by the entity's discriminator value
    }
    public String getPrinterModel() {
        return printerModel;
    }

    public void setPrinterModel(String printerModel) {
        this.printerModel = printerModel;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public ConnectionConfig getConnectionConfig() {
        return connectionConfig;
    }

    public void setConnectionConfig(ConnectionConfig connectionConfig) {
        this.connectionConfig = connectionConfig;
    }

    public boolean isAutoConnectOnStartup() {
        return autoConnectOnStartup;
    }

    public void setAutoConnectOnStartup(boolean autoConnectOnStartup) {
        this.autoConnectOnStartup = autoConnectOnStartup;
    }
}
