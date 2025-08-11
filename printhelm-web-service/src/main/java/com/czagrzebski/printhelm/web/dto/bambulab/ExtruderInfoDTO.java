package com.czagrzebski.printhelm.web.dto.bambulab;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class ExtruderInfoDTO {
    @JsonProperty("filam_bak")
    private List<Object> filamBak;
    private int hnow;
    private int hpre;
    private int htar;
    private int id;
    private int info;
    private int snow;
    private int spre;
    private int star;
    private int stat;
    private int temp;

    public List<Object> getFilamBak() {
        return filamBak;
    }

    public void setFilamBak(List<Object> filamBak) {
        this.filamBak = filamBak;
    }

    public int getHnow() {
        return hnow;
    }

    public void setHnow(int hnow) {
        this.hnow = hnow;
    }

    public int getHpre() {
        return hpre;
    }

    public void setHpre(int hpre) {
        this.hpre = hpre;
    }

    public int getHtar() {
        return htar;
    }

    public void setHtar(int htar) {
        this.htar = htar;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getInfo() {
        return info;
    }

    public void setInfo(int info) {
        this.info = info;
    }

    public int getSnow() {
        return snow;
    }

    public void setSnow(int snow) {
        this.snow = snow;
    }

    public int getSpre() {
        return spre;
    }

    public void setSpre(int spre) {
        this.spre = spre;
    }

    public int getStar() {
        return star;
    }

    public void setStar(int star) {
        this.star = star;
    }

    public int getStat() {
        return stat;
    }

    public void setStat(int stat) {
        this.stat = stat;
    }

    public int getTemp() {
        return temp;
    }

    public void setTemp(int temp) {
        this.temp = temp;
    }
}
