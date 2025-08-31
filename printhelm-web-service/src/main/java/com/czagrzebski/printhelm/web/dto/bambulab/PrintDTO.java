package com.czagrzebski.printhelm.web.dto.bambulab;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PrintDTO {
    public ThreeDTO getThreeD() {
        return threeD;
    }

    public void setThreeD(ThreeDTO threeD) {
        this.threeD = threeD;
    }

    public AmsDTO getAms() {
        return ams;
    }

    public void setAms(AmsDTO ams) {
        this.ams = ams;
    }

    public int getAmsRfidStatus() {
        return amsRfidStatus;
    }

    public void setAmsRfidStatus(int amsRfidStatus) {
        this.amsRfidStatus = amsRfidStatus;
    }

    public int getAmsStatus() {
        return amsStatus;
    }

    public void setAmsStatus(int amsStatus) {
        this.amsStatus = amsStatus;
    }

    public int getApErr() {
        return apErr;
    }

    public void setApErr(int apErr) {
        this.apErr = apErr;
    }

    public String getAux() {
        return aux;
    }

    public void setAux(String aux) {
        this.aux = aux;
    }

    public boolean isAuxPartFan() {
        return auxPartFan;
    }

    public void setAuxPartFan(boolean auxPartFan) {
        this.auxPartFan = auxPartFan;
    }

    public int getBatchId() {
        return batchId;
    }

    public void setBatchId(int batchId) {
        this.batchId = batchId;
    }

    public double getBedTargetTemper() {
        return bedTargetTemper;
    }

    public void setBedTargetTemper(double bedTargetTemper) {
        this.bedTargetTemper = bedTargetTemper;
    }

    public double getBedTemper() {
        return bedTemper;
    }

    public void setBedTemper(double bedTemper) {
        this.bedTemper = bedTemper;
    }

    public String getBigFan1Speed() {
        return bigFan1Speed;
    }

    public void setBigFan1Speed(String bigFan1Speed) {
        this.bigFan1Speed = bigFan1Speed;
    }

    public String getBigFan2Speed() {
        return bigFan2Speed;
    }

    public void setBigFan2Speed(String bigFan2Speed) {
        this.bigFan2Speed = bigFan2Speed;
    }

    public int getCaliVersion() {
        return caliVersion;
    }

    public void setCaliVersion(int caliVersion) {
        this.caliVersion = caliVersion;
    }

    public int getCanvasId() {
        return canvasId;
    }

    public void setCanvasId(int canvasId) {
        this.canvasId = canvasId;
    }

    public List<CareDTO> getCare() {
        return care;
    }

    public void setCare(List<CareDTO> care) {
        this.care = care;
    }

    public String getCfg() {
        return cfg;
    }

    public void setCfg(String cfg) {
        this.cfg = cfg;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getCoolingFanSpeed() {
        return coolingFanSpeed;
    }

    public void setCoolingFanSpeed(String coolingFanSpeed) {
        this.coolingFanSpeed = coolingFanSpeed;
    }

    public String getDesignId() {
        return designId;
    }

    public void setDesignId(String designId) {
        this.designId = designId;
    }

    public DeviceDTO getDevice() {
        return device;
    }

    public void setDevice(DeviceDTO device) {
        this.device = device;
    }

    public String getErr() {
        return err;
    }

    public void setErr(String err) {
        this.err = err;
    }

    public String getFailReason() {
        return failReason;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }

    public int getFanGear() {
        return fanGear;
    }

    public void setFanGear(int fanGear) {
        this.fanGear = fanGear;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public boolean isForceUpgrade() {
        return forceUpgrade;
    }

    public void setForceUpgrade(boolean forceUpgrade) {
        this.forceUpgrade = forceUpgrade;
    }

    public String getFun() {
        return fun;
    }

    public void setFun(String fun) {
        this.fun = fun;
    }

    public String getGcodeFile() {
        return gcodeFile;
    }

    public void setGcodeFile(String gcodeFile) {
        this.gcodeFile = gcodeFile;
    }

    public String getGcodeFilePreparePercent() {
        return gcodeFilePreparePercent;
    }

    public void setGcodeFilePreparePercent(String gcodeFilePreparePercent) {
        this.gcodeFilePreparePercent = gcodeFilePreparePercent;
    }

    public String getGcodeState() {
        return gcodeState;
    }

    public void setGcodeState(String gcodeState) {
        this.gcodeState = gcodeState;
    }

    public String getHeatbreakFanSpeed() {
        return heatbreakFanSpeed;
    }

    public void setHeatbreakFanSpeed(String heatbreakFanSpeed) {
        this.heatbreakFanSpeed = heatbreakFanSpeed;
    }

    public List<Object> getHms() {
        return hms;
    }

    public void setHms(List<Object> hms) {
        this.hms = hms;
    }

    public int getHomeFlag() {
        return homeFlag;
    }

    public void setHomeFlag(int homeFlag) {
        this.homeFlag = homeFlag;
    }

    public int getHwSwitchState() {
        return hwSwitchState;
    }

    public void setHwSwitchState(int hwSwitchState) {
        this.hwSwitchState = hwSwitchState;
    }

    public InfoDTO getInfo() {
        return info;
    }

    public void setInfo(InfoDTO info) {
        this.info = info;
    }

    public IpcamDTO getIpcam() {
        return ipcam;
    }

    public void setIpcam(IpcamDTO ipcam) {
        this.ipcam = ipcam;
    }

    public JobDTO getJob() {
        return job;
    }

    public void setJob(JobDTO job) {
        this.job = job;
    }

    public int getJobAttr() {
        return jobAttr;
    }

    public void setJobAttr(int jobAttr) {
        this.jobAttr = jobAttr;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public int getLayerNum() {
        return layerNum;
    }

    public void setLayerNum(int layerNum) {
        this.layerNum = layerNum;
    }

    public List<LightsReportDTO> getLightsReport() {
        return lightsReport;
    }

    public void setLightsReport(List<LightsReportDTO> lightsReport) {
        this.lightsReport = lightsReport;
    }

    public List<Integer> getMapping() {
        return mapping;
    }

    public void setMapping(List<Integer> mapping) {
        this.mapping = mapping;
    }

    public int getMcAction() {
        return mcAction;
    }

    public void setMcAction(int mcAction) {
        this.mcAction = mcAction;
    }

    public int getMcErr() {
        return mcErr;
    }

    public void setMcErr(int mcErr) {
        this.mcErr = mcErr;
    }

    public int getMcPercent() {
        return mcPercent;
    }

    public void setMcPercent(int mcPercent) {
        this.mcPercent = mcPercent;
    }

    public String getMcPrintErrorCode() {
        return mcPrintErrorCode;
    }

    public void setMcPrintErrorCode(String mcPrintErrorCode) {
        this.mcPrintErrorCode = mcPrintErrorCode;
    }

    public String getMcPrintStage() {
        return mcPrintStage;
    }

    public void setMcPrintStage(String mcPrintStage) {
        this.mcPrintStage = mcPrintStage;
    }

    public int getMcPrintSubStage() {
        return mcPrintSubStage;
    }

    public void setMcPrintSubStage(int mcPrintSubStage) {
        this.mcPrintSubStage = mcPrintSubStage;
    }

    public int getMcRemainingTime() {
        return mcRemainingTime;
    }

    public void setMcRemainingTime(int mcRemainingTime) {
        this.mcRemainingTime = mcRemainingTime;
    }

    public int getMcStage() {
        return mcStage;
    }

    public void setMcStage(int mcStage) {
        this.mcStage = mcStage;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelId) {
        this.modelId = modelId;
    }

    public NetDTO getNet() {
        return net;
    }

    public void setNet(NetDTO net) {
        this.net = net;
    }

    public String getNozzleDiameter() {
        return nozzleDiameter;
    }

    public void setNozzleDiameter(String nozzleDiameter) {
        this.nozzleDiameter = nozzleDiameter;
    }

    public double getNozzleTargetTemper() {
        return nozzleTargetTemper;
    }

    public void setNozzleTargetTemper(double nozzleTargetTemper) {
        this.nozzleTargetTemper = nozzleTargetTemper;
    }

    public double getNozzleTemper() {
        return nozzleTemper;
    }

    public void setNozzleTemper(double nozzleTemper) {
        this.nozzleTemper = nozzleTemper;
    }

    public String getNozzleType() {
        return nozzleType;
    }

    public void setNozzleType(String nozzleType) {
        this.nozzleType = nozzleType;
    }

    public OnlineDTO getOnline() {
        return online;
    }

    public void setOnline(OnlineDTO online) {
        this.online = online;
    }

    public int getPercent() {
        return percent;
    }

    public void setPercent(int percent) {
        this.percent = percent;
    }

    public int getPlateCnt() {
        return plateCnt;
    }

    public void setPlateCnt(int plateCnt) {
        this.plateCnt = plateCnt;
    }

    public int getPlateId() {
        return plateId;
    }

    public void setPlateId(int plateId) {
        this.plateId = plateId;
    }

    public int getPlateIdx() {
        return plateIdx;
    }

    public void setPlateIdx(int plateIdx) {
        this.plateIdx = plateIdx;
    }

    public int getPreparePer() {
        return preparePer;
    }

    public void setPreparePer(int preparePer) {
        this.preparePer = preparePer;
    }

    public int getPrintError() {
        return printError;
    }

    public void setPrintError(int printError) {
        this.printError = printError;
    }

    public int getPrintGcodeAction() {
        return printGcodeAction;
    }

    public void setPrintGcodeAction(int printGcodeAction) {
        this.printGcodeAction = printGcodeAction;
    }

    public int getPrintRealAction() {
        return printRealAction;
    }

    public void setPrintRealAction(int printRealAction) {
        this.printRealAction = printRealAction;
    }

    public String getPrintType() {
        return printType;
    }

    public void setPrintType(String printType) {
        this.printType = printType;
    }

    public String getProfileId() {
        return profileId;
    }

    public void setProfileId(String profileId) {
        this.profileId = profileId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public int getQueue() {
        return queue;
    }

    public void setQueue(int queue) {
        this.queue = queue;
    }

    public int getQueueEst() {
        return queueEst;
    }

    public void setQueueEst(int queueEst) {
        this.queueEst = queueEst;
    }

    public int getQueueNumber() {
        return queueNumber;
    }

    public void setQueueNumber(int queueNumber) {
        this.queueNumber = queueNumber;
    }

    public int getQueueSts() {
        return queueSts;
    }

    public void setQueueSts(int queueSts) {
        this.queueSts = queueSts;
    }

    public int getQueueTotal() {
        return queueTotal;
    }

    public void setQueueTotal(int queueTotal) {
        this.queueTotal = queueTotal;
    }

    public int getRemainTime() {
        return remainTime;
    }

    public void setRemainTime(int remainTime) {
        this.remainTime = remainTime;
    }

    public List<Object> getsObj() {
        return sObj;
    }

    public void setsObj(List<Object> sObj) {
        this.sObj = sObj;
    }

    public boolean isSdcard() {
        return sdcard;
    }

    public void setSdcard(boolean sdcard) {
        this.sdcard = sdcard;
    }

    public String getSequenceId() {
        return sequenceId;
    }

    public void setSequenceId(String sequenceId) {
        this.sequenceId = sequenceId;
    }

    public int getSpdLvl() {
        return spdLvl;
    }

    public void setSpdLvl(int spdLvl) {
        this.spdLvl = spdLvl;
    }

    public int getSpdMag() {
        return spdMag;
    }

    public void setSpdMag(int spdMag) {
        this.spdMag = spdMag;
    }

    public String getStat() {
        return stat;
    }

    public void setStat(String stat) {
        this.stat = stat;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public List<Integer> getStg() {
        return stg;
    }

    public void setStg(List<Integer> stg) {
        this.stg = stg;
    }

    public int getStgCur() {
        return stgCur;
    }

    public void setStgCur(int stgCur) {
        this.stgCur = stgCur;
    }

    public String getSubtaskId() {
        return subtaskId;
    }

    public void setSubtaskId(String subtaskId) {
        this.subtaskId = subtaskId;
    }

    public String getSubtaskName() {
        return subtaskName;
    }

    public void setSubtaskName(String subtaskName) {
        this.subtaskName = subtaskName;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public int getTotalLayerNum() {
        return totalLayerNum;
    }

    public void setTotalLayerNum(int totalLayerNum) {
        this.totalLayerNum = totalLayerNum;
    }

    public UpgradeStateDTO getUpgradeState() {
        return upgradeState;
    }

    public void setUpgradeState(UpgradeStateDTO upgradeState) {
        this.upgradeState = upgradeState;
    }

    public UploadDTO getUpload() {
        return upload;
    }

    public void setUpload(UploadDTO upload) {
        this.upload = upload;
    }

    public String getVer() {
        return ver;
    }

    public void setVer(String ver) {
        this.ver = ver;
    }

    public List<TrayDTO> getVirSlot() {
        return virSlot;
    }

    public void setVirSlot(List<TrayDTO> virSlot) {
        this.virSlot = virSlot;
    }

    public TrayDTO getVtTray() {
        return vtTray;
    }

    public void setVtTray(TrayDTO vtTray) {
        this.vtTray = vtTray;
    }

    public String getWifiSignal() {
        return wifiSignal;
    }

    public void setWifiSignal(String wifiSignal) {
        this.wifiSignal = wifiSignal;
    }

    public XcamDTO getXcam() {
        return xcam;
    }

    public void setXcam(XcamDTO xcam) {
        this.xcam = xcam;
    }

    public String getXcamStatus() {
        return xcamStatus;
    }

    public void setXcamStatus(String xcamStatus) {
        this.xcamStatus = xcamStatus;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @JsonProperty("3D")
    private ThreeDTO threeD;
    private AmsDTO ams;
    @JsonProperty("ams_rfid_status")
    private int amsRfidStatus;
    @JsonProperty("ams_status")
    private int amsStatus;
    @JsonProperty("ap_err")
    private int apErr;
    private String aux;
    @JsonProperty("aux_part_fan")
    private boolean auxPartFan;
    @JsonProperty("batch_id")
    private int batchId;
    @JsonProperty("bed_target_temper")
    private double bedTargetTemper;
    @JsonProperty("bed_temper")
    private double bedTemper;
    @JsonProperty("big_fan1_speed")
    private String bigFan1Speed;
    @JsonProperty("big_fan2_speed")
    private String bigFan2Speed;
    @JsonProperty("cali_version")
    private int caliVersion;
    @JsonProperty("canvas_id")
    private int canvasId;
    private List<CareDTO> care;
    private String cfg;
    private String command;
    @JsonProperty("cooling_fan_speed")
    private String coolingFanSpeed;
    @JsonProperty("design_id")
    private String designId;
    private DeviceDTO device;
    private String err;
    @JsonProperty("fail_reason")
    private String failReason;
    @JsonProperty("fan_gear")
    private int fanGear;
    private String file;
    @JsonProperty("force_upgrade")
    private boolean forceUpgrade;
    private String fun;
    @JsonProperty("gcode_file")
    private String gcodeFile;
    @JsonProperty("gcode_file_prepare_percent")
    private String gcodeFilePreparePercent;
    @JsonProperty("gcode_state")
    private String gcodeState;
    @JsonProperty("heatbreak_fan_speed")
    private String heatbreakFanSpeed;
    private List<Object> hms;
    @JsonProperty("home_flag")
    private int homeFlag;
    @JsonProperty("hw_switch_state")
    private int hwSwitchState;
    private InfoDTO info;
    private IpcamDTO ipcam;
    private JobDTO job;
    @JsonProperty("job_attr")
    private int jobAttr;
    @JsonProperty("job_id")
    private String jobId;
    @JsonProperty("layer_num")
    private int layerNum;
    @JsonProperty("lights_report")
    private List<LightsReportDTO> lightsReport;
    private List<Integer> mapping;
    @JsonProperty("mc_action")
    private int mcAction;
    @JsonProperty("mc_err")
    private int mcErr;
    @JsonProperty("mc_percent")
    private int mcPercent;
    @JsonProperty("mc_print_error_code")
    private String mcPrintErrorCode;
    @JsonProperty("mc_print_stage")
    private String mcPrintStage;
    @JsonProperty("mc_print_sub_stage")
    private int mcPrintSubStage;
    @JsonProperty("mc_remaining_time")
    private int mcRemainingTime;
    @JsonProperty("mc_stage")
    private int mcStage;
    @JsonProperty("model_id")
    private String modelId;
    private NetDTO net;
    @JsonProperty("nozzle_diameter")
    private String nozzleDiameter;
    @JsonProperty("nozzle_target_temper")
    private double nozzleTargetTemper;
    @JsonProperty("nozzle_temper")
    private double nozzleTemper;
    @JsonProperty("nozzle_type")
    private String nozzleType;
    private OnlineDTO online;
    private int percent;
    @JsonProperty("plate_cnt")
    private int plateCnt;
    @JsonProperty("plate_id")
    private int plateId;
    @JsonProperty("plate_idx")
    private int plateIdx;
    @JsonProperty("prepare_per")
    private int preparePer;
    @JsonProperty("print_error")
    private int printError;
    @JsonProperty("print_gcode_action")
    private int printGcodeAction;
    @JsonProperty("print_real_action")
    private int printRealAction;
    @JsonProperty("print_type")
    private String printType;
    @JsonProperty("profile_id")
    private String profileId;
    @JsonProperty("project_id")
    private String projectId;
    private int queue;
    @JsonProperty("queue_est")
    private int queueEst;
    @JsonProperty("queue_number")
    private int queueNumber;
    @JsonProperty("queue_sts")
    private int queueSts;
    @JsonProperty("queue_total")
    private int queueTotal;
    @JsonProperty("remain_time")
    private int remainTime;
    @JsonProperty("s_obj")
    private List<Object> sObj;
    private boolean sdcard;
    @JsonProperty("sequence_id")
    private String sequenceId;
    @JsonProperty("spd_lvl")
    private int spdLvl;
    @JsonProperty("spd_mag")
    private int spdMag;
    private String stat;
    private int state;
    private List<Integer> stg;
    @JsonProperty("stg_cur")
    private int stgCur;
    @JsonProperty("subtask_id")
    private String subtaskId;
    @JsonProperty("subtask_name")
    private String subtaskName;
    @JsonProperty("task_id")
    private String taskId;
    @JsonProperty("total_layer_num")
    private int totalLayerNum;
    @JsonProperty("upgrade_state")
    private UpgradeStateDTO upgradeState;
    private UploadDTO upload;
    private String ver;
    @JsonProperty("vir_slot")
    private List<TrayDTO> virSlot;
    @JsonProperty("vt_tray")
    private TrayDTO vtTray;
    @JsonProperty("wifi_signal")
    private String wifiSignal;
    private XcamDTO xcam;
    @JsonProperty("xcam_status")
    private String xcamStatus;
    private String reason;

}
