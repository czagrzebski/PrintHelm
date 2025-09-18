package com.czagrzebski.printhelm.web.dto.bambulab;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceDTO {
    private BedDTO bed;
    @JsonProperty("bed_temp")
    private int bedTemp;
    private CamDTO cam;
    private CtcDTO ctc;
    @JsonProperty("ext_tool")
    private ExtToolDTO extTool;
    private ExtruderDTO extruder;
    private int fan;
    private LaserDTO laser;
    private NozzleDTO nozzle;

    public PlateDTO getPlate() {
        return plate;
    }

    public void setPlate(PlateDTO plate) {
        this.plate = plate;
    }

    public BedDTO getBed() {
        return bed;
    }

    public void setBed(BedDTO bed) {
        this.bed = bed;
    }

    public int getBedTemp() {
        return bedTemp;
    }

    public void setBedTemp(int bedTemp) {
        this.bedTemp = bedTemp;
    }

    public CamDTO getCam() {
        return cam;
    }

    public void setCam(CamDTO cam) {
        this.cam = cam;
    }

    public CtcDTO getCtc() {
        return ctc;
    }

    public void setCtc(CtcDTO ctc) {
        this.ctc = ctc;
    }

    public ExtToolDTO getExtTool() {
        return extTool;
    }

    public void setExtTool(ExtToolDTO extTool) {
        this.extTool = extTool;
    }

    public ExtruderDTO getExtruder() {
        return extruder;
    }

    public void setExtruder(ExtruderDTO extruder) {
        this.extruder = extruder;
    }

    public int getFan() {
        return fan;
    }

    public void setFan(int fan) {
        this.fan = fan;
    }

    public LaserDTO getLaser() {
        return laser;
    }

    public void setLaser(LaserDTO laser) {
        this.laser = laser;
    }

    public NozzleDTO getNozzle() {
        return nozzle;
    }

    public void setNozzle(NozzleDTO nozzle) {
        this.nozzle = nozzle;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    private PlateDTO plate;
    private int type;
}
