package com.czagrzebski.printhelm.web.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

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
    @Column(name = "status", nullable = false)
    private JobOrderStatus status = JobOrderStatus.SUBMITTED;

    @Column(name = "requirements", length = 2000)
    private String requirements;

    @Column(name = "requires_custom_design")
    private Boolean requiresCustomDesign;

    @Column(name = "mongo_part_file_id", length = 50)
    private String mongoPartFileId;

    @Column(name = "mongo_gcode_file_id", length = 50)
    private String mongoGcodeFileId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

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

    public String getMongoGcodeFileId() { return mongoGcodeFileId; }
    public void setMongoGcodeFileId(String mongoGcodeFileId) { this.mongoGcodeFileId = mongoGcodeFileId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
