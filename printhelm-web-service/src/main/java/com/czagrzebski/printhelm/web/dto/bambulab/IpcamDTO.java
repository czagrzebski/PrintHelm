package com.czagrzebski.printhelm.web.dto.bambulab;

import com.fasterxml.jackson.annotation.JsonProperty;

public class IpcamDTO {
    @JsonProperty("agora_service")
    private String agoraService;
    @JsonProperty("brtc_service")
    private String brtcService;
    @JsonProperty("bs_state")
    private int bsState;
    @JsonProperty("ipcam_dev")
    private String ipcamDev;
    @JsonProperty("ipcam_record")
    private String ipcamRecord;
    @JsonProperty("laser_preview_res")
    private int laserPreviewRes;
    @JsonProperty("mode_bits")
    private int modeBits;
    private String resolution;
    @JsonProperty("rtsp_url")
    private String rtspUrl;
    private String timelapse;
    @JsonProperty("tl_store_hpd_type")
    private int tlStoreHpdType;
    @JsonProperty("tl_store_path_type")
    private int tlStorePathType;
    @JsonProperty("tutk_server")
    private String tutkServer;

    public String getAgoraService() {
        return agoraService;
    }

    public void setAgoraService(String agoraService) {
        this.agoraService = agoraService;
    }

    public String getBrtcService() {
        return brtcService;
    }

    public void setBrtcService(String brtcService) {
        this.brtcService = brtcService;
    }

    public int getBsState() {
        return bsState;
    }

    public void setBsState(int bsState) {
        this.bsState = bsState;
    }

    public String getIpcamDev() {
        return ipcamDev;
    }

    public void setIpcamDev(String ipcamDev) {
        this.ipcamDev = ipcamDev;
    }

    public String getIpcamRecord() {
        return ipcamRecord;
    }

    public void setIpcamRecord(String ipcamRecord) {
        this.ipcamRecord = ipcamRecord;
    }

    public int getLaserPreviewRes() {
        return laserPreviewRes;
    }

    public void setLaserPreviewRes(int laserPreviewRes) {
        this.laserPreviewRes = laserPreviewRes;
    }

    public int getModeBits() {
        return modeBits;
    }

    public void setModeBits(int modeBits) {
        this.modeBits = modeBits;
    }

    public String getResolution() {
        return resolution;
    }

    public void setResolution(String resolution) {
        this.resolution = resolution;
    }

    public String getRtspUrl() {
        return rtspUrl;
    }

    public void setRtspUrl(String rtspUrl) {
        this.rtspUrl = rtspUrl;
    }

    public String getTimelapse() {
        return timelapse;
    }

    public void setTimelapse(String timelapse) {
        this.timelapse = timelapse;
    }

    public int getTlStoreHpdType() {
        return tlStoreHpdType;
    }

    public void setTlStoreHpdType(int tlStoreHpdType) {
        this.tlStoreHpdType = tlStoreHpdType;
    }

    public int getTlStorePathType() {
        return tlStorePathType;
    }

    public void setTlStorePathType(int tlStorePathType) {
        this.tlStorePathType = tlStorePathType;
    }

    public String getTutkServer() {
        return tutkServer;
    }

    public void setTutkServer(String tutkServer) {
        this.tutkServer = tutkServer;
    }
}
