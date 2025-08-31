package com.czagrzebski.printhelm.web.dto.bambulab;

public class NetInfoDTO {
    public int getIp() {
        return ip;
    }

    public void setIp(int ip) {
        this.ip = ip;
    }

    public int getMask() {
        return mask;
    }

    public void setMask(int mask) {
        this.mask = mask;
    }

    private int ip;
    private int mask;
}
