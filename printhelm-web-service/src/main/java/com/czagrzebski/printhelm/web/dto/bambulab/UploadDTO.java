package com.czagrzebski.printhelm.web.dto.bambulab;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UploadDTO {
    @JsonProperty("file_size")
    private int fileSize;
    @JsonProperty("finish_size")
    private int finishSize;
    private String message;
    @JsonProperty("oss_url")
    private String ossUrl;
    private int progress;
    @JsonProperty("sequence_id")
    private String sequenceId;
    private int speed;
    private String status;
    @JsonProperty("task_id")
    private String taskId;
    @JsonProperty("time_remaining")
    private int timeRemaining;
    @JsonProperty("trouble_id")
    private String troubleId;

    public int getFileSize() {
        return fileSize;
    }

    public void setFileSize(int fileSize) {
        this.fileSize = fileSize;
    }

    public int getFinishSize() {
        return finishSize;
    }

    public void setFinishSize(int finishSize) {
        this.finishSize = finishSize;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOssUrl() {
        return ossUrl;
    }

    public void setOssUrl(String ossUrl) {
        this.ossUrl = ossUrl;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public String getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(String sequenceId) {
        this.sequenceId = sequenceId;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public int getTimeRemaining() {
        return timeRemaining;
    }

    public void setTimeRemaining(int timeRemaining) {
        this.timeRemaining = timeRemaining;
    }

    public String getTroubleId() {
        return troubleId;
    }

    public void setTroubleId(String troubleId) {
        this.troubleId = troubleId;
    }
}
