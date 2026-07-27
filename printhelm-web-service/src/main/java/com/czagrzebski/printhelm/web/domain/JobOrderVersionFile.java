package com.czagrzebski.printhelm.web.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** One file within a {@link JobOrderFileVersion}; part versions may contain several assembly files. */
@Embeddable
public class JobOrderVersionFile {

    @Column(name = "filename", length = 255, nullable = false)
    private String filename;

    @Column(name = "mongo_file_id", length = 50, nullable = false)
    private String mongoFileId;

    @Column(name = "mongo_gcode_metadata_id", length = 50)
    private String mongoGcodeMetadataId;

    // Print plan (gcode files): nullable so rows created before plan support read as qty 1 / 0 done
    @Column(name = "print_quantity")
    private Integer printQuantity;

    @Column(name = "completed_count")
    private Integer completedCount;

    public JobOrderVersionFile() {
    }

    public JobOrderVersionFile(String filename, String mongoFileId) {
        this(filename, mongoFileId, null);
    }

    public JobOrderVersionFile(String filename, String mongoFileId, String mongoGcodeMetadataId) {
        this.filename = filename;
        this.mongoFileId = mongoFileId;
        this.mongoGcodeMetadataId = mongoGcodeMetadataId;
    }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getMongoFileId() { return mongoFileId; }
    public void setMongoFileId(String mongoFileId) { this.mongoFileId = mongoFileId; }

    public String getMongoGcodeMetadataId() { return mongoGcodeMetadataId; }
    public void setMongoGcodeMetadataId(String mongoGcodeMetadataId) { this.mongoGcodeMetadataId = mongoGcodeMetadataId; }

    public Integer getPrintQuantity() { return printQuantity; }
    public void setPrintQuantity(Integer printQuantity) { this.printQuantity = printQuantity; }

    public Integer getCompletedCount() { return completedCount; }
    public void setCompletedCount(Integer completedCount) { this.completedCount = completedCount; }

    /** Required prints, defaulting to 1 for rows created before print-plan support */
    public int effectiveQuantity() { return printQuantity != null ? printQuantity : 1; }

    public int effectiveCompleted() { return completedCount != null ? completedCount : 0; }

    public boolean isPlanComplete() { return effectiveCompleted() >= effectiveQuantity(); }
}
