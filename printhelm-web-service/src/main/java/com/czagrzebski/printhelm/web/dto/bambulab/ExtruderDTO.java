package com.czagrzebski.printhelm.web.dto.bambulab;

import java.util.List;

public class ExtruderDTO {
    public List<ExtruderInfoDTO> getInfo() {
        return info;
    }

    public void setInfo(List<ExtruderInfoDTO> info) {
        this.info = info;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    private List<ExtruderInfoDTO> info;
    private int state;
}
