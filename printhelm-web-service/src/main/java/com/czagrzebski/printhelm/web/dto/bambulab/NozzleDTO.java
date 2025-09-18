package com.czagrzebski.printhelm.web.dto.bambulab;

import java.util.List;

public class NozzleDTO {
    private int exist;

    public List<NozzleInfoDTO> getInfo() {
        return info;
    }

    public void setInfo(List<NozzleInfoDTO> info) {
        this.info = info;
    }

    public int getExist() {
        return exist;
    }

    public void setExist(int exist) {
        this.exist = exist;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    private List<NozzleInfoDTO> info;
    private int state;
}
