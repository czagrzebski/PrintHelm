package com.czagrzebski.printhelm.web.dto.bambulab;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class AmsDTO {
    private List<AmsItemDTO> ams;
    @JsonProperty("ams_exist_bits")
    private String amsExistBits;
    @JsonProperty("ams_exist_bits_raw")
    private String amsExistBitsRaw;
    @JsonProperty("cali_id")
    private int caliId;
    @JsonProperty("cali_stat")
    private int caliStat;
    @JsonProperty("insert_flag")
    private boolean insertFlag;
    @JsonProperty("power_on_flag")
    private boolean powerOnFlag;
    @JsonProperty("tray_exist_bits")
    private String trayExistBits;
    @JsonProperty("tray_is_bbl_bits")
    private String trayIsBblBits;
    @JsonProperty("tray_now")
    private String trayNow;
    @JsonProperty("tray_pre")
    private String trayPre;
    @JsonProperty("tray_read_done_bits")
    private String trayReadDoneBits;
    @JsonProperty("tray_reading_bits")
    private String trayReadingBits;
    @JsonProperty("tray_tar")
    private String trayTar;
    @JsonProperty("unbind_ams_stat")
    private int unbindAmsStat;
    private int version;

    public List<AmsItemDTO> getAms() {
        return ams;
    }

    public void setAms(List<AmsItemDTO> ams) {
        this.ams = ams;
    }

    public String getAmsExistBits() {
        return amsExistBits;
    }

    public void setAmsExistBits(String amsExistBits) {
        this.amsExistBits = amsExistBits;
    }

    public String getAmsExistBitsRaw() {
        return amsExistBitsRaw;
    }

    public void setAmsExistBitsRaw(String amsExistBitsRaw) {
        this.amsExistBitsRaw = amsExistBitsRaw;
    }

    public int getCaliId() {
        return caliId;
    }

    public void setCaliId(int caliId) {
        this.caliId = caliId;
    }

    public int getCaliStat() {
        return caliStat;
    }

    public void setCaliStat(int caliStat) {
        this.caliStat = caliStat;
    }

    public boolean isInsertFlag() {
        return insertFlag;
    }

    public void setInsertFlag(boolean insertFlag) {
        this.insertFlag = insertFlag;
    }

    public boolean isPowerOnFlag() {
        return powerOnFlag;
    }

    public void setPowerOnFlag(boolean powerOnFlag) {
        this.powerOnFlag = powerOnFlag;
    }

    public String getTrayExistBits() {
        return trayExistBits;
    }

    public void setTrayExistBits(String trayExistBits) {
        this.trayExistBits = trayExistBits;
    }

    public String getTrayIsBblBits() {
        return trayIsBblBits;
    }

    public void setTrayIsBblBits(String trayIsBblBits) {
        this.trayIsBblBits = trayIsBblBits;
    }

    public String getTrayNow() {
        return trayNow;
    }

    public void setTrayNow(String trayNow) {
        this.trayNow = trayNow;
    }

    public String getTrayPre() {
        return trayPre;
    }

    public void setTrayPre(String trayPre) {
        this.trayPre = trayPre;
    }

    public String getTrayReadDoneBits() {
        return trayReadDoneBits;
    }

    public void setTrayReadDoneBits(String trayReadDoneBits) {
        this.trayReadDoneBits = trayReadDoneBits;
    }

    public String getTrayReadingBits() {
        return trayReadingBits;
    }

    public void setTrayReadingBits(String trayReadingBits) {
        this.trayReadingBits = trayReadingBits;
    }

    public String getTrayTar() {
        return trayTar;
    }

    public void setTrayTar(String trayTar) {
        this.trayTar = trayTar;
    }

    public int getUnbindAmsStat() {
        return unbindAmsStat;
    }

    public void setUnbindAmsStat(int unbindAmsStat) {
        this.unbindAmsStat = unbindAmsStat;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
