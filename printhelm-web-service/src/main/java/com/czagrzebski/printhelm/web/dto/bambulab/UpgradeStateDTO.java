package com.czagrzebski.printhelm.web.dto.bambulab;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpgradeStateDTO {
    @JsonProperty("ahb_new_version_number")
    private String ahbNewVersionNumber;
    @JsonProperty("ams_new_version_number")
    private String amsNewVersionNumber;
    @JsonProperty("consistency_request")
    private boolean consistencyRequest;
    @JsonProperty("dis_state")
    private int disState;
    @JsonProperty("err_code")
    private int errCode;
    @JsonProperty("ext_new_version_number")
    private String extNewVersionNumber;
    @JsonProperty("force_upgrade")
    private boolean forceUpgrade;
    private int idx;
    private int idx2;
    @JsonProperty("lower_limit")
    private String lowerLimit;
    private String message;
    private String module;
    @JsonProperty("new_version_state")
    private int newVersionState;
    @JsonProperty("ota_new_version_number")
    private String otaNewVersionNumber;
    private String progress;
    @JsonProperty("sequence_id")
    private int sequenceId;
    private String sn;
    private String status;

    public String getAhbNewVersionNumber() {
        return ahbNewVersionNumber;
    }

    public void setAhbNewVersionNumber(String ahbNewVersionNumber) {
        this.ahbNewVersionNumber = ahbNewVersionNumber;
    }

    public String getAmsNewVersionNumber() {
        return amsNewVersionNumber;
    }

    public void setAmsNewVersionNumber(String amsNewVersionNumber) {
        this.amsNewVersionNumber = amsNewVersionNumber;
    }

    public boolean isConsistencyRequest() {
        return consistencyRequest;
    }

    public void setConsistencyRequest(boolean consistencyRequest) {
        this.consistencyRequest = consistencyRequest;
    }

    public int getDisState() {
        return disState;
    }

    public void setDisState(int disState) {
        this.disState = disState;
    }

    public int getErrCode() {
        return errCode;
    }

    public void setErrCode(int errCode) {
        this.errCode = errCode;
    }

    public String getExtNewVersionNumber() {
        return extNewVersionNumber;
    }

    public void setExtNewVersionNumber(String extNewVersionNumber) {
        this.extNewVersionNumber = extNewVersionNumber;
    }

    public boolean isForceUpgrade() {
        return forceUpgrade;
    }

    public void setForceUpgrade(boolean forceUpgrade) {
        this.forceUpgrade = forceUpgrade;
    }

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

    public int getIdx2() {
        return idx2;
    }

    public void setIdx2(int idx2) {
        this.idx2 = idx2;
    }

    public String getLowerLimit() {
        return lowerLimit;
    }

    public void setLowerLimit(String lowerLimit) {
        this.lowerLimit = lowerLimit;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public int getNewVersionState() {
        return newVersionState;
    }

    public void setNewVersionState(int newVersionState) {
        this.newVersionState = newVersionState;
    }

    public String getOtaNewVersionNumber() {
        return otaNewVersionNumber;
    }

    public void setOtaNewVersionNumber(String otaNewVersionNumber) {
        this.otaNewVersionNumber = otaNewVersionNumber;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public int getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(int sequenceId) {
        this.sequenceId = sequenceId;
    }

    public String getSn() {
        return sn;
    }

    public void setSn(String sn) {
        this.sn = sn;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
