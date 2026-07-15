package com.czagrzebski.printhelm.web.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "JobOrder")
public class JobOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "customer_name", length = 100, nullable = false)
    private String customerName;

    @Column(name = "customer_email", length = 150)
    private String customerEmail;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar(50)")
    private JobOrderStatus status = JobOrderStatus.SUBMITTED;

    @Column(name = "requirements", length = 2000)
    private String requirements;

    @Column(name = "requires_custom_design")
    private Boolean requiresCustomDesign;

    @Column(name = "mongo_part_file_id", length = 50)
    private String mongoPartFileId;

    @Column(name = "part_filename", length = 255)
    private String partFilename;

    @Column(name = "mongo_gcode_file_id", length = 50)
    private String mongoGcodeFileId;

    @Column(name = "gcode_filename", length = 255)
    private String gcodeFilename;

    @Column(name = "mongo_gcode_metadata_id", length = 50)
    private String mongoGcodeMetadataId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_printer_id")
    private Printer assignedPrinter;

    @Column(name = "queue_position")
    private Integer queuePosition;

    @Column(name = "assigned_filename", length = 200)
    private String assignedFilename;

    @Column(name = "print_started_at")
    private LocalDateTime printStartedAt;

    @Column(name = "quoted_material_cost", precision = 10, scale = 2)
    private BigDecimal quotedMaterialCost;

    @Column(name = "quoted_cost_per_unit", precision = 10, scale = 4)
    private BigDecimal quotedCostPerUnit;

    @Column(name = "quoted_quantity")
    private Integer quotedQuantity;

    @Column(name = "quoted_labor_cost", precision = 10, scale = 2)
    private BigDecimal quotedLaborCost;

    @Column(name = "quoted_setup_fee", precision = 10, scale = 2)
    private BigDecimal quotedSetupFee;

    @Column(name = "quoted_discount", precision = 10, scale = 2)
    private BigDecimal quotedDiscount;

    @Column(name = "quote_notes", length = 2000)
    private String quoteNotes;

    @Column(name = "quoted_at")
    private LocalDateTime quotedAt;

    @Column(name = "quote_expires_at")
    private LocalDate quoteExpiresAt;

    @Convert(converter = QuoteLineItemsConverter.class)
    @Column(name = "quote_line_items", columnDefinition = "TEXT")
    private List<QuoteLineItem> quoteLineItems;

    @Convert(converter = StringListConverter.class)
    @Column(name = "quote_materials", columnDefinition = "TEXT")
    private List<String> quoteMaterials;

    @Column(name = "quoted_print_time_hours", precision = 10, scale = 2)
    private BigDecimal quotedPrintTimeHours;

    @Column(name = "quoted_filament_grams", precision = 10, scale = 2)
    private BigDecimal quotedFilamentGrams;

    @Column(name = "quoted_lead_time_days")
    private Integer quotedLeadTimeDays;

    @Column(name = "design_notes", length = 2000)
    private String designNotes;

    @Column(name = "material_cost", precision = 10, scale = 2)
    private BigDecimal materialCost;

    @Column(name = "labor_cost", precision = 10, scale = 2)
    private BigDecimal laborCost;

    @Column(name = "setup_fee", precision = 10, scale = 2)
    private BigDecimal setupFee;

    @Column(name = "discount", precision = 10, scale = 2)
    private BigDecimal discount;

    @Column(name = "invoice_notes", length = 2000)
    private String invoiceNotes;

    @Column(name = "invoiced_at")
    private LocalDateTime invoicedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public JobOrderStatus getStatus() { return status; }
    public void setStatus(JobOrderStatus status) { this.status = status; }

    public String getRequirements() { return requirements; }
    public void setRequirements(String requirements) { this.requirements = requirements; }

    public Boolean getRequiresCustomDesign() { return requiresCustomDesign; }
    public void setRequiresCustomDesign(Boolean requiresCustomDesign) { this.requiresCustomDesign = requiresCustomDesign; }

    public String getMongoPartFileId() { return mongoPartFileId; }
    public void setMongoPartFileId(String mongoPartFileId) { this.mongoPartFileId = mongoPartFileId; }

    public String getPartFilename() { return partFilename; }
    public void setPartFilename(String partFilename) { this.partFilename = partFilename; }

    public String getMongoGcodeFileId() { return mongoGcodeFileId; }
    public void setMongoGcodeFileId(String mongoGcodeFileId) { this.mongoGcodeFileId = mongoGcodeFileId; }

    public String getGcodeFilename() { return gcodeFilename; }
    public void setGcodeFilename(String gcodeFilename) { this.gcodeFilename = gcodeFilename; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Printer getAssignedPrinter() { return assignedPrinter; }
    public void setAssignedPrinter(Printer assignedPrinter) { this.assignedPrinter = assignedPrinter; }

    public Integer getQueuePosition() { return queuePosition; }
    public void setQueuePosition(Integer queuePosition) { this.queuePosition = queuePosition; }

    public String getAssignedFilename() { return assignedFilename; }
    public void setAssignedFilename(String assignedFilename) { this.assignedFilename = assignedFilename; }

    public String getMongoGcodeMetadataId() { return mongoGcodeMetadataId; }
    public void setMongoGcodeMetadataId(String mongoGcodeMetadataId) { this.mongoGcodeMetadataId = mongoGcodeMetadataId; }

    public LocalDateTime getPrintStartedAt() { return printStartedAt; }
    public void setPrintStartedAt(LocalDateTime printStartedAt) { this.printStartedAt = printStartedAt; }

    public BigDecimal getQuotedMaterialCost() { return quotedMaterialCost; }
    public void setQuotedMaterialCost(BigDecimal quotedMaterialCost) { this.quotedMaterialCost = quotedMaterialCost; }

    public BigDecimal getQuotedCostPerUnit() { return quotedCostPerUnit; }
    public void setQuotedCostPerUnit(BigDecimal quotedCostPerUnit) { this.quotedCostPerUnit = quotedCostPerUnit; }

    public Integer getQuotedQuantity() { return quotedQuantity; }
    public void setQuotedQuantity(Integer quotedQuantity) { this.quotedQuantity = quotedQuantity; }

    public BigDecimal getQuotedLaborCost() { return quotedLaborCost; }
    public void setQuotedLaborCost(BigDecimal quotedLaborCost) { this.quotedLaborCost = quotedLaborCost; }

    public BigDecimal getQuotedSetupFee() { return quotedSetupFee; }
    public void setQuotedSetupFee(BigDecimal quotedSetupFee) { this.quotedSetupFee = quotedSetupFee; }

    public BigDecimal getQuotedDiscount() { return quotedDiscount; }
    public void setQuotedDiscount(BigDecimal quotedDiscount) { this.quotedDiscount = quotedDiscount; }

    public String getQuoteNotes() { return quoteNotes; }
    public void setQuoteNotes(String quoteNotes) { this.quoteNotes = quoteNotes; }

    public LocalDateTime getQuotedAt() { return quotedAt; }
    public void setQuotedAt(LocalDateTime quotedAt) { this.quotedAt = quotedAt; }

    public LocalDate getQuoteExpiresAt() { return quoteExpiresAt; }
    public void setQuoteExpiresAt(LocalDate quoteExpiresAt) { this.quoteExpiresAt = quoteExpiresAt; }

    public List<QuoteLineItem> getQuoteLineItems() { return quoteLineItems; }
    public void setQuoteLineItems(List<QuoteLineItem> quoteLineItems) { this.quoteLineItems = quoteLineItems; }

    public List<String> getQuoteMaterials() { return quoteMaterials; }
    public void setQuoteMaterials(List<String> quoteMaterials) { this.quoteMaterials = quoteMaterials; }

    public BigDecimal getQuotedPrintTimeHours() { return quotedPrintTimeHours; }
    public void setQuotedPrintTimeHours(BigDecimal quotedPrintTimeHours) { this.quotedPrintTimeHours = quotedPrintTimeHours; }

    public BigDecimal getQuotedFilamentGrams() { return quotedFilamentGrams; }
    public void setQuotedFilamentGrams(BigDecimal quotedFilamentGrams) { this.quotedFilamentGrams = quotedFilamentGrams; }

    public Integer getQuotedLeadTimeDays() { return quotedLeadTimeDays; }
    public void setQuotedLeadTimeDays(Integer quotedLeadTimeDays) { this.quotedLeadTimeDays = quotedLeadTimeDays; }

    public String getDesignNotes() { return designNotes; }
    public void setDesignNotes(String designNotes) { this.designNotes = designNotes; }

    public BigDecimal getMaterialCost() { return materialCost; }
    public void setMaterialCost(BigDecimal materialCost) { this.materialCost = materialCost; }

    public BigDecimal getLaborCost() { return laborCost; }
    public void setLaborCost(BigDecimal laborCost) { this.laborCost = laborCost; }

    public BigDecimal getSetupFee() { return setupFee; }
    public void setSetupFee(BigDecimal setupFee) { this.setupFee = setupFee; }

    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }

    public String getInvoiceNotes() { return invoiceNotes; }
    public void setInvoiceNotes(String invoiceNotes) { this.invoiceNotes = invoiceNotes; }

    public LocalDateTime getInvoicedAt() { return invoicedAt; }
    public void setInvoicedAt(LocalDateTime invoicedAt) { this.invoicedAt = invoicedAt; }
}
