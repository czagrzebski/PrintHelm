package com.czagrzebski.printhelm.web.dto.bambulab;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AmsItemDTO {
    public int getDryTime() {
        return dryTime;
    }

    public void setDryTime(int dryTime) {
        this.dryTime = dryTime;
    }

    public String getHumidity() {
        return humidity;
    }

    public void setHumidity(String humidity) {
        this.humidity = humidity;
    }

    public String getHumidityRaw() {
        return humidityRaw;
    }

    public void setHumidityRaw(String humidityRaw) {
        this.humidityRaw = humidityRaw;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getTemp() {
        return temp;
    }

    public void setTemp(String temp) {
        this.temp = temp;
    }

    public List<TrayDTO> getTray() {
        return tray;
    }

    public void setTray(List<TrayDTO> tray) {
        this.tray = tray;
    }

    @JsonProperty("dry_time")
    private int dryTime;
    private String humidity;
    @JsonProperty("humidity_raw")
    private String humidityRaw;
    private String id;
    private String info;
    private String temp;
    private List<TrayDTO> tray;
}
