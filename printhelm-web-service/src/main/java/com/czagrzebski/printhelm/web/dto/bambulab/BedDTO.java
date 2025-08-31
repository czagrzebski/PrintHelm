package com.czagrzebski.printhelm.web.dto.bambulab;

public class BedDTO {
    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public InfoDTO getInfo() {
        return info;
    }

    public void setInfo(InfoDTO info) {
        this.info = info;
    }

    private InfoDTO info;
    private int state;
}
