package com.czagrzebski.printhelm.web.mapper;

import com.czagrzebski.printhelm.model.ApiFan;
import com.czagrzebski.printhelm.model.ApiLight;
import com.czagrzebski.printhelm.model.ApiMaterial;
import com.czagrzebski.printhelm.model.ApiMaterialSystem;
import com.czagrzebski.printhelm.model.ApiPrinterState;
import com.czagrzebski.printhelm.web.domain.bambulab.BambuLabPrinter;
import com.czagrzebski.printhelm.web.dto.bambulab.AmsDTO;
import com.czagrzebski.printhelm.web.dto.bambulab.AmsItemDTO;
import com.czagrzebski.printhelm.web.dto.bambulab.BambulabStateDTO;
import com.czagrzebski.printhelm.web.dto.bambulab.LightsReportDTO;
import com.czagrzebski.printhelm.web.dto.bambulab.PrintDTO;
import com.czagrzebski.printhelm.web.dto.bambulab.TrayDTO;
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
    public abstract ApiPrinterState bambuPrinterStateToPrinterState(BambulabStateDTO stateDTO);

    @Named("state")
    protected String getStateFromMcStage(PrintDTO printDTO) {
        return BambuLabPrinter.PrintStage.fromCode(printDTO.getStgCur()).getDescription();
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

        fans.add(bigFan1);
        fans.add(bigFan2);
        fans.add(coolingFan);

        return fans;
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
                materials.add(material);
            }
        }
        apiMaterialSystem.setMaterials(materials);
        return apiMaterialSystem;
    }
}
