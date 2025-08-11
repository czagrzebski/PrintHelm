package com.czagrzebski.printhelm.web.dto.bambulab;

public class NozzleInfoDTO {
    private double diameter;
    private int id;
    private int tm;
    private String type;
    private int wear;

    public double getDiameter() {
        return diameter;
    }

    public void setDiameter(double diameter) {
        this.diameter = diameter;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTm() {
        return tm;
    }

    public void setTm(int tm) {
        this.tm = tm;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getWear() {
        return wear;
    }

    public void setWear(int wear) {
        this.wear = wear;
    }
}
