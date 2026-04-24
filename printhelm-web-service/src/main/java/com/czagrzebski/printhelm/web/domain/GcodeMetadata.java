package com.czagrzebski.printhelm.web.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document(collection = "gcode_metadata")
public class GcodeMetadata {

    @Id
    private String id;
    private long orderId;
    private List<GcodeFilamentInfo> filaments;
    private boolean multiColor;
    private int colorCount;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public long getOrderId() { return orderId; }
    public void setOrderId(long orderId) { this.orderId = orderId; }

    public List<GcodeFilamentInfo> getFilaments() { return filaments; }
    public void setFilaments(List<GcodeFilamentInfo> filaments) { this.filaments = filaments; }

    public boolean isMultiColor() { return multiColor; }
    public void setMultiColor(boolean multiColor) { this.multiColor = multiColor; }

    public int getColorCount() { return colorCount; }
    public void setColorCount(int colorCount) { this.colorCount = colorCount; }
}
