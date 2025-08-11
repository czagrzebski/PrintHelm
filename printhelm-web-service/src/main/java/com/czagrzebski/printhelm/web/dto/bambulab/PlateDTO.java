package com.czagrzebski.printhelm.web.dto.bambulab;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlateDTO {
    private int base;
    @JsonProperty("cali2d_id")
    private String cali2dId;
    @JsonProperty("cur_id")
    private String curId;
    private int mat;
    @JsonProperty("tar_id")
    private String tarId;

    public int getBase() {
        return base;
    }

    public void setBase(int base) {
        this.base = base;
    }

    public String getCali2dId() {
        return cali2dId;
    }

    public void setCali2dId(String cali2dId) {
        this.cali2dId = cali2dId;
    }

    public String getCurId() {
        return curId;
    }

    public void setCurId(String curId) {
        this.curId = curId;
    }

    public int getMat() {
        return mat;
    }

    public void setMat(int mat) {
        this.mat = mat;
    }

    public String getTarId() {
        return tarId;
    }

    public void setTarId(String tarId) {
        this.tarId = tarId;
    }
}
