package com.czagrzebski.printhelm.web.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "JobOrderFileVersion")
public class JobOrderFileVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "version_id")
    private Long versionId;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, columnDefinition = "varchar(20)")
    private JobOrderFileType fileType;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "filename", length = 255, nullable = false)
    private String filename;

    @Column(name = "mongo_file_id", length = 50, nullable = false)
    private String mongoFileId;

    @Column(name = "mongo_gcode_metadata_id", length = 50)
    private String mongoGcodeMetadataId;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public Long getVersionId() { return versionId; }
    public void setVersionId(Long versionId) { this.versionId = versionId; }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public JobOrderFileType getFileType() { return fileType; }
    public void setFileType(JobOrderFileType fileType) { this.fileType = fileType; }

    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getMongoFileId() { return mongoFileId; }
    public void setMongoFileId(String mongoFileId) { this.mongoFileId = mongoFileId; }

    public String getMongoGcodeMetadataId() { return mongoGcodeMetadataId; }
    public void setMongoGcodeMetadataId(String mongoGcodeMetadataId) { this.mongoGcodeMetadataId = mongoGcodeMetadataId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
