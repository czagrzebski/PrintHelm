package com.czagrzebski.printhelm.web.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "FilamentSpool")
public class FilamentSpool {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "spool_id")
    private Long spoolId;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "brand", length = 100)
    private String brand;

    @Column(name = "material", length = 50, nullable = false)
    private String material;

    @Column(name = "color_hex", length = 9)
    private String colorHex;

    @Column(name = "color_name", length = 50)
    private String colorName;

    @Column(name = "diameter")
    private Double diameter = 1.75;

    @Column(name = "initial_weight_grams", nullable = false)
    private Double initialWeightGrams;

    @Column(name = "remaining_weight_grams", nullable = false)
    private Double remainingWeightGrams;

    @Column(name = "spool_cost", precision = 10, scale = 2)
    private BigDecimal spoolCost;

    @Column(name = "low_stock_threshold_grams")
    private Double lowStockThresholdGrams = 200.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "varchar(20)")
    private FilamentSpoolStatus status = FilamentSpoolStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_printer_id")
    private Printer assignedPrinter;

    @Column(name = "ams_slot")
    private Integer amsSlot;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getSpoolId() { return spoolId; }
    public void setSpoolId(Long spoolId) { this.spoolId = spoolId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public String getColorHex() { return colorHex; }
    public void setColorHex(String colorHex) { this.colorHex = colorHex; }

    public String getColorName() { return colorName; }
    public void setColorName(String colorName) { this.colorName = colorName; }

    public Double getDiameter() { return diameter; }
    public void setDiameter(Double diameter) { this.diameter = diameter; }

    public Double getInitialWeightGrams() { return initialWeightGrams; }
    public void setInitialWeightGrams(Double initialWeightGrams) { this.initialWeightGrams = initialWeightGrams; }

    public Double getRemainingWeightGrams() { return remainingWeightGrams; }
    public void setRemainingWeightGrams(Double remainingWeightGrams) { this.remainingWeightGrams = remainingWeightGrams; }

    public BigDecimal getSpoolCost() { return spoolCost; }
    public void setSpoolCost(BigDecimal spoolCost) { this.spoolCost = spoolCost; }

    public Double getLowStockThresholdGrams() { return lowStockThresholdGrams; }
    public void setLowStockThresholdGrams(Double lowStockThresholdGrams) { this.lowStockThresholdGrams = lowStockThresholdGrams; }

    public FilamentSpoolStatus getStatus() { return status; }
    public void setStatus(FilamentSpoolStatus status) { this.status = status; }

    public Printer getAssignedPrinter() { return assignedPrinter; }
    public void setAssignedPrinter(Printer assignedPrinter) { this.assignedPrinter = assignedPrinter; }

    public Integer getAmsSlot() { return amsSlot; }
    public void setAmsSlot(Integer amsSlot) { this.amsSlot = amsSlot; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
