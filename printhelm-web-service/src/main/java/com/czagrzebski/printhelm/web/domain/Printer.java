package com.czagrzebski.printhelm.web.domain;

import com.czagrzebski.printhelm.web.domain.connection.ConnectionConfig;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="Printer")
@Inheritance(strategy = InheritanceType.JOINED)
public abstract class Printer {
    @Id
    @Column(name="printer_id")
    private String printerId;

    @Column(name="printer_name", length=50, nullable=false, unique=false)
    private String printerName;

    @Column(name="printer_type", length=50, nullable=false, unique=false)
    private String printerType;

    @Column(name="printer_model", length=50, nullable=false, unique=false)
    private String printerModel;

    @Column(name="location", length=50, nullable=false, unique=false)
    private String location;

    @Column(name="serial_number", length=50, nullable=false, unique=false)
    private String serialNumber;

    @OneToOne(mappedBy = "printer", cascade = CascadeType.ALL)
    private ConnectionConfig connectionConfig;



}
