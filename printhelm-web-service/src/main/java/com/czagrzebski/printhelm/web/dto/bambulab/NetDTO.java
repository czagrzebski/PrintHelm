package com.czagrzebski.printhelm.web.dto.bambulab;

import java.util.List;

public class NetDTO {
    private int conf;

    public List<NetInfoDTO> getInfo() {
        return info;
    }

    public void setInfo(List<NetInfoDTO> info) {
        this.info = info;
    }

    public int getConf() {
        return conf;
    }

    public void setConf(int conf) {
        this.conf = conf;
    }

    private List<NetInfoDTO> info;
}
