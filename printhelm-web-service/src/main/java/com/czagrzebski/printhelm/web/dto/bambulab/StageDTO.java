package com.czagrzebski.printhelm.web.dto.bambulab;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class StageDTO {
    @JsonProperty("clock_in")
    private boolean clockIn;
    private List<String> color;
    private List<Double> diameter;
    @JsonProperty("est_time")
    private int estTime;
    private double heigh;
    private int idx;
    private String platform;
    @JsonProperty("print_then")
    private boolean printThen;
    @JsonProperty("proc_list")
    private List<Object> procList;
    private List<String> tool;
    private int type;

    public boolean isClockIn() {
        return clockIn;
    }

    public void setClockIn(boolean clockIn) {
        this.clockIn = clockIn;
    }

    public List<String> getColor() {
        return color;
    }

    public void setColor(List<String> color) {
        this.color = color;
    }

    public List<Double> getDiameter() {
        return diameter;
    }

    public void setDiameter(List<Double> diameter) {
        this.diameter = diameter;
    }

    public int getEstTime() {
        return estTime;
    }

    public void setEstTime(int estTime) {
        this.estTime = estTime;
    }

    public double getHeigh() {
        return heigh;
    }

    public void setHeigh(double heigh) {
        this.heigh = heigh;
    }

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public boolean isPrintThen() {
        return printThen;
    }

    public void setPrintThen(boolean printThen) {
        this.printThen = printThen;
    }

    public List<Object> getProcList() {
        return procList;
    }

    public void setProcList(List<Object> procList) {
        this.procList = procList;
    }

    public List<String> getTool() {
        return tool;
    }

    public void setTool(List<String> tool) {
        this.tool = tool;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }
}
