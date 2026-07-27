package com.czagrzebski.printhelm.web.dto.bambulab;

public class NetInfoDTO {
    private long ip;
    private long mask;

    public long getIp() {
        return ip;
    }

    public void setIp(long ip) {
        this.ip = ip;
    }

    public long getMask() {
        return mask;
    }

    public void setMask(long mask) {
        this.mask = mask;
    }
}
