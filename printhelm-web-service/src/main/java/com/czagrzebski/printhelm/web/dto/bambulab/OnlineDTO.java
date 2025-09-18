package com.czagrzebski.printhelm.web.dto.bambulab;

public class OnlineDTO {
    private boolean ahb;
    private boolean ext;
    private int version;

    public boolean isAhb() {
        return ahb;
    }

    public void setAhb(boolean ahb) {
        this.ahb = ahb;
    }

    public boolean isExt() {
        return ext;
    }

    public void setExt(boolean ext) {
        this.ext = ext;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
