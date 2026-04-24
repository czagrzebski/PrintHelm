package com.czagrzebski.printhelm.web.mapper;

import com.czagrzebski.printhelm.model.ApiFan;
import com.czagrzebski.printhelm.model.ApiIpcam;
import com.czagrzebski.printhelm.model.ApiLight;
import com.czagrzebski.printhelm.model.ApiMaterial;
import com.czagrzebski.printhelm.model.ApiMaterialSystem;
import com.czagrzebski.printhelm.model.ApiPrinterState;
import com.czagrzebski.printhelm.model.ApiUpgradeState;
import com.czagrzebski.printhelm.model.ApiXcam;
import com.czagrzebski.printhelm.web.domain.bambulab.BambuLabPrinter;
import com.czagrzebski.printhelm.web.dto.bambulab.AmsDTO;
import com.czagrzebski.printhelm.web.dto.bambulab.AmsItemDTO;
import com.czagrzebski.printhelm.web.dto.bambulab.BambulabStateDTO;
import com.czagrzebski.printhelm.web.dto.bambulab.IpcamDTO;
import com.czagrzebski.printhelm.web.dto.bambulab.LightsReportDTO;
import com.czagrzebski.printhelm.web.dto.bambulab.PrintDTO;
import com.czagrzebski.printhelm.web.dto.bambulab.TrayDTO;
import com.czagrzebski.printhelm.web.dto.bambulab.UpgradeStateDTO;
import com.czagrzebski.printhelm.web.dto.bambulab.XcamDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.ArrayList;
import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public abstract class BambuPrinterStateMapper {

    @Mapping(target = "bedTemp", source = "stateDTO.print.bedTemper")
    @Mapping(target = "file", source = "stateDTO.print.file")
    @Mapping(target = "nozzleTemp", source = "stateDTO.print.nozzleTemper")
    @Mapping(target = "progress", source = "stateDTO.print.percent")
    @Mapping(target = "wifiSignalStrength", source = "stateDTO.print.wifiSignal")
    @Mapping(target = "materialSystem", source = "stateDTO.print.ams")
    @Mapping(target = "nozzleTargetTemp", source = "stateDTO.print.nozzleTargetTemper")
    @Mapping(target = "bedTargetTemp", source = "stateDTO.print.bedTargetTemper")
    @Mapping(target = "nozzleDiameter", source = "stateDTO.print.nozzleDiameter")
    @Mapping(target = "nozzleType", source = "stateDTO.print.nozzleType")
    @Mapping(target = "state", source = "stateDTO.print", qualifiedByName = "state")
    @Mapping(target = "currentLayer", source = "stateDTO.print.threeD.layerNum")
    @Mapping(target = "totalLayers", source = "stateDTO.print.threeD.totalLayerNum")
    @Mapping(target = "fans", source = "stateDTO.print", qualifiedByName = "fans")
    @Mapping(target = "lights", source = "stateDTO.print.lightsReport")
    @Mapping(target = "remainTime", source = "stateDTO.print.mcRemainingTime")
    @Mapping(target = "gcodeState", source = "stateDTO.print.gcodeState")
    @Mapping(target = "printType", source = "stateDTO.print.printType")
    @Mapping(target = "taskId", source = "stateDTO.print.taskId")
    @Mapping(target = "jobId", source = "stateDTO.print.jobId")
    @Mapping(target = "projectId", source = "stateDTO.print.projectId")
    @Mapping(target = "profileId", source = "stateDTO.print.profileId")
    @Mapping(target = "modelId", source = "stateDTO.print.modelId")
    @Mapping(target = "subtaskId", source = "stateDTO.print.subtaskId")
    @Mapping(target = "sdcard", source = "stateDTO.print.sdcard")
    @Mapping(target = "homeFlag", source = "stateDTO.print.homeFlag")
    @Mapping(target = "spdLvl", source = "stateDTO.print.spdLvl")
    @Mapping(target = "spdMag", source = "stateDTO.print.spdMag")
    @Mapping(target = "printError", source = "stateDTO.print.printError")
    @Mapping(target = "mcPrintErrorCode", source = "stateDTO.print", qualifiedByName = "mcPrintErrorCode")
    @Mapping(target = "failReason", source = "stateDTO.print", qualifiedByName = "failReason")
    @Mapping(target = "ipcam", source = "stateDTO.print.ipcam", qualifiedByName = "ipcam")
    @Mapping(target = "xcam", source = "stateDTO.print.xcam", qualifiedByName = "xcam")
    @Mapping(target = "upgradeState", source = "stateDTO.print.upgradeState", qualifiedByName = "upgradeState")
    public abstract ApiPrinterState bambuPrinterStateToPrinterState(BambulabStateDTO stateDTO);

    @Named("mcPrintErrorCode")
    protected String getMcPrintErrorCode(PrintDTO printDTO) {
        String code = printDTO.getMcPrintErrorCode();
        return (code == null || code.equals("0") || code.isBlank()) ? null : code;
    }

    @Named("failReason")
    protected String getFailReason(PrintDTO printDTO) {
        String reason = printDTO.getFailReason();
        return (reason == null || reason.equals("0") || reason.isBlank()) ? null : reason;
    }

    @Named("state")
    protected String getStateFromMcStage(PrintDTO printDTO) {
        BambuLabPrinter.PrintStage stage = BambuLabPrinter.PrintStage.fromCode(printDTO.getStgCur());
        return stage != null ? stage.getDescription() : "Unknown (" + printDTO.getStgCur() + ")";
    }

    @Named("fans")
    protected List<ApiFan> bambuFansToFans(PrintDTO printDTO) {
        List<ApiFan> fans = new ArrayList<>();

        var bigFan1 = new ApiFan();
        bigFan1.setName("Auxiliary Cooling");
        bigFan1.setSpeed(printDTO.getBigFan1Speed());

        var bigFan2 = new ApiFan();
        bigFan2.setName("Chamber");
        bigFan2.setSpeed(printDTO.getBigFan2Speed());

        var coolingFan = new ApiFan();
        coolingFan.setName("Part Cooling Fan");
        coolingFan.setSpeed(printDTO.getCoolingFanSpeed());

        var heatbreakFan = new ApiFan();
        heatbreakFan.setName("Heatbreak");
        heatbreakFan.setSpeed(printDTO.getHeatbreakFanSpeed());

        fans.add(bigFan1);
        fans.add(bigFan2);
        fans.add(coolingFan);
        fans.add(heatbreakFan);

        return fans;
    }

    @Named("ipcam")
    protected ApiIpcam bambuIpcamToApiIpcam(IpcamDTO ipcamDTO) {
        if (ipcamDTO == null) return null;
        ApiIpcam apiIpcam = new ApiIpcam();
        apiIpcam.setResolution(ipcamDTO.getResolution());
        apiIpcam.setRtspUrl(ipcamDTO.getRtspUrl());
        apiIpcam.setTimelapse(ipcamDTO.getTimelapse());
        apiIpcam.setIpcamRecord(ipcamDTO.getIpcamRecord());
        apiIpcam.setIpcamDev(ipcamDTO.getIpcamDev());
        return apiIpcam;
    }

    @Named("xcam")
    protected ApiXcam bambuXcamToApiXcam(XcamDTO xcamDTO) {
        if (xcamDTO == null) return null;
        ApiXcam apiXcam = new ApiXcam();
        apiXcam.setFirstLayerInspector(xcamDTO.isFirstLayerInspector());
        apiXcam.setBuildplateMarkerDetector(xcamDTO.isBuildplateMarkerDetector());
        apiXcam.setSpaghettiDetector(xcamDTO.isSpaghettiDetector());
        apiXcam.setPrintingMonitor(xcamDTO.isPrintingMonitor());
        apiXcam.setPrintHalt(xcamDTO.isPrintHalt());
        apiXcam.setHaltPrintSensitivity(xcamDTO.getHaltPrintSensitivity());
        apiXcam.setAllowSkipParts(xcamDTO.isAllowSkipParts());
        return apiXcam;
    }

    @Named("upgradeState")
    protected ApiUpgradeState bambuUpgradeStateToApiUpgradeState(UpgradeStateDTO upgradeStateDTO) {
        if (upgradeStateDTO == null) return null;
        ApiUpgradeState apiUpgradeState = new ApiUpgradeState();
        apiUpgradeState.setStatus(upgradeStateDTO.getStatus());
        apiUpgradeState.setProgress(upgradeStateDTO.getProgress());
        apiUpgradeState.setOtaNewVersionNumber(upgradeStateDTO.getOtaNewVersionNumber());
        apiUpgradeState.setAmsNewVersionNumber(upgradeStateDTO.getAmsNewVersionNumber());
        apiUpgradeState.setAhbNewVersionNumber(upgradeStateDTO.getAhbNewVersionNumber());
        apiUpgradeState.setExtNewVersionNumber(upgradeStateDTO.getExtNewVersionNumber());
        apiUpgradeState.setMessage(upgradeStateDTO.getMessage());
        apiUpgradeState.setForceUpgrade(upgradeStateDTO.isForceUpgrade());
        return apiUpgradeState;
    }

    @Mapping(target = "name", source = "lightsReportDTO.node")
    @Mapping(target = "state", source = "lightsReportDTO.mode")
    public abstract ApiLight bambuLightsReportToApiLightList(LightsReportDTO lightsReportDTO);

    protected ApiMaterialSystem bambuMaterialSystemToApiMaterialSystem(AmsDTO amsDTO) {
        ApiMaterialSystem apiMaterialSystem = new ApiMaterialSystem();
        List<ApiMaterial> materials = new ArrayList<>();

        for(AmsItemDTO amsItemDTO : amsDTO.getAms()) {
            apiMaterialSystem.setHumidity(amsItemDTO.getHumidityRaw());
            apiMaterialSystem.setTemperature(amsItemDTO.getTemp());
            for(TrayDTO tray : amsItemDTO.getTray()) {
                var selectedTray = (Integer.parseInt(amsItemDTO.getId()) * 4) + tray.getId();
                ApiMaterial material = new ApiMaterial();
                if(tray.getTraySubBrands() != null) {
                    material.setName(tray.getTraySubBrands());
                    material.setColor(tray.getTrayColor());
                } else {
                    material.setName("Unknown Material");
                }
                material.setLoaded(amsDTO.getTrayNow().equals(selectedTray));
                material.setType(tray.getTrayType());
                material.setRemain(tray.getRemain());
                material.setTrayDiameter(tray.getTrayDiameter());
                material.setTrayWeight(tray.getTrayWeight());
                material.setTrayUuid(tray.getTrayUuid());
                material.setNozzleTempMin(tray.getNozzleTempMin());
                material.setNozzleTempMax(tray.getNozzleTempMax());
                material.setRecommendedBedTemp(tray.getBedTemp());
                material.setDryingTemp(tray.getDryingTemp());
                material.setDryingTime(tray.getDryingTime());
                materials.add(material);
            }
        }
        apiMaterialSystem.setMaterials(materials);
        return apiMaterialSystem;
    }
}
