package com.czagrzebski.printhelm.web.dto.bambulab;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class TrayDTO {
    @JsonProperty("bed_temp")
    private String bedTemp;
    @JsonProperty("bed_temp_type")
    private String bedTempType;
    @JsonProperty("cali_idx")
    private int caliIdx;
    private List<String> cols;
    private int ctype;
    @JsonProperty("drying_temp")
    private String dryingTemp;
    @JsonProperty("drying_time")
    private String dryingTime;
    private String id;
    @JsonProperty("nozzle_temp_max")
    private String nozzleTempMax;
    @JsonProperty("nozzle_temp_min")
    private String nozzleTempMin;
    private int remain;
    private int state;
    @JsonProperty("tag_uid")
    private String tagUid;
    @JsonProperty("total_len")
    private int totalLen;
    @JsonProperty("tray_color")
    private String trayColor;

    public String getTrayDiameter() {
        return trayDiameter;
    }

    public void setTrayDiameter(String trayDiameter) {
        this.trayDiameter = trayDiameter;
    }

    public String getBedTemp() {
        return bedTemp;
    }

    public void setBedTemp(String bedTemp) {
        this.bedTemp = bedTemp;
    }

    public String getBedTempType() {
        return bedTempType;
    }

    public void setBedTempType(String bedTempType) {
        this.bedTempType = bedTempType;
    }

    public int getCaliIdx() {
        return caliIdx;
    }

    public void setCaliIdx(int caliIdx) {
        this.caliIdx = caliIdx;
    }

    public List<String> getCols() {
        return cols;
    }

    public void setCols(List<String> cols) {
        this.cols = cols;
    }

    public int getCtype() {
        return ctype;
    }

    public void setCtype(int ctype) {
        this.ctype = ctype;
    }

    public String getDryingTemp() {
        return dryingTemp;
    }

    public void setDryingTemp(String dryingTemp) {
        this.dryingTemp = dryingTemp;
    }

    public String getDryingTime() {
        return dryingTime;
    }

    public void setDryingTime(String dryingTime) {
        this.dryingTime = dryingTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNozzleTempMax() {
        return nozzleTempMax;
    }

    public void setNozzleTempMax(String nozzleTempMax) {
        this.nozzleTempMax = nozzleTempMax;
    }

    public String getNozzleTempMin() {
        return nozzleTempMin;
    }

    public void setNozzleTempMin(String nozzleTempMin) {
        this.nozzleTempMin = nozzleTempMin;
    }

    public int getRemain() {
        return remain;
    }

    public void setRemain(int remain) {
        this.remain = remain;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getTagUid() {
        return tagUid;
    }

    public void setTagUid(String tagUid) {
        this.tagUid = tagUid;
    }

    public int getTotalLen() {
        return totalLen;
    }

    public void setTotalLen(int totalLen) {
        this.totalLen = totalLen;
    }

    public String getTrayColor() {
        return trayColor;
    }

    public void setTrayColor(String trayColor) {
        this.trayColor = trayColor;
    }

    public String getTrayIdName() {
        return trayIdName;
    }

    public void setTrayIdName(String trayIdName) {
        this.trayIdName = trayIdName;
    }

    public String getTrayInfoIdx() {
        return trayInfoIdx;
    }

    public void setTrayInfoIdx(String trayInfoIdx) {
        this.trayInfoIdx = trayInfoIdx;
    }

    public String getTraySubBrands() {
        return traySubBrands;
    }

    public void setTraySubBrands(String traySubBrands) {
        this.traySubBrands = traySubBrands;
    }

    public String getTrayType() {
        return trayType;
    }

    public void setTrayType(String trayType) {
        this.trayType = trayType;
    }

    public String getTrayUuid() {
        return trayUuid;
    }

    public void setTrayUuid(String trayUuid) {
        this.trayUuid = trayUuid;
    }

    public String getTrayWeight() {
        return trayWeight;
    }

    public void setTrayWeight(String trayWeight) {
        this.trayWeight = trayWeight;
    }

    public String getXcamInfo() {
        return xcamInfo;
    }

    public void setXcamInfo(String xcamInfo) {
        this.xcamInfo = xcamInfo;
    }

    @JsonProperty("tray_diameter")
    private String trayDiameter;
    @JsonProperty("tray_id_name")
    private String trayIdName;
    @JsonProperty("tray_info_idx")
    private String trayInfoIdx;
    @JsonProperty("tray_sub_brands")
    private String traySubBrands;
    @JsonProperty("tray_type")
    private String trayType;
    @JsonProperty("tray_uuid")
    private String trayUuid;
    @JsonProperty("tray_weight")
    private String trayWeight;
    @JsonProperty("xcam_info")
    private String xcamInfo;
}
