package com.czagrzebski.printhelm.web.dto.bambulab;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ExtToolDTO {
    private int calib;
    @JsonProperty("low_prec")
    private boolean lowPrec;
    private int mount;
    @JsonProperty("th_temp")
    private int thTemp;
    private String type;

    public int getCalib() {
        return calib;
    }

    public void setCalib(int calib) {
        this.calib = calib;
    }

    public boolean isLowPrec() {
        return lowPrec;
    }

    public void setLowPrec(boolean lowPrec) {
        this.lowPrec = lowPrec;
    }

    public int getMount() {
        return mount;
    }

    public void setMount(int mount) {
        this.mount = mount;
    }

    public int getThTemp() {
        return thTemp;
    }

    public void setThTemp(int thTemp) {
        this.thTemp = thTemp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
