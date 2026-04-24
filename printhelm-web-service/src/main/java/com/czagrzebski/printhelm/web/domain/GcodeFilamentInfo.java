package com.czagrzebski.printhelm.web.domain;

public class GcodeFilamentInfo {

    private int slotIndex;
    private String type;
    private String color;

    public GcodeFilamentInfo() {}

    public GcodeFilamentInfo(int slotIndex, String type, String color) {
        this.slotIndex = slotIndex;
        this.type = type;
        this.color = color;
    }

    public int getSlotIndex() { return slotIndex; }
    public void setSlotIndex(int slotIndex) { this.slotIndex = slotIndex; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
