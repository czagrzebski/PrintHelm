package com.czagrzebski.printhelm.web.dto.bambulab;

import com.fasterxml.jackson.annotation.JsonProperty;

public class XcamDTO {
    @JsonProperty("allow_skip_parts")
    private boolean allowSkipParts;
    @JsonProperty("buildplate_marker_detector")
    private boolean buildplateMarkerDetector;
    @JsonProperty("first_layer_inspector")
    private boolean firstLayerInspector;
    @JsonProperty("halt_print_sensitivity")
    private String haltPrintSensitivity;
    @JsonProperty("print_halt")
    private boolean printHalt;
    @JsonProperty("printing_monitor")
    private boolean printingMonitor;
    @JsonProperty("spaghetti_detector")
    private boolean spaghettiDetector;

    public boolean isAllowSkipParts() {
        return allowSkipParts;
    }

    public void setAllowSkipParts(boolean allowSkipParts) {
        this.allowSkipParts = allowSkipParts;
    }

    public boolean isBuildplateMarkerDetector() {
        return buildplateMarkerDetector;
    }

    public void setBuildplateMarkerDetector(boolean buildplateMarkerDetector) {
        this.buildplateMarkerDetector = buildplateMarkerDetector;
    }

    public boolean isFirstLayerInspector() {
        return firstLayerInspector;
    }

    public void setFirstLayerInspector(boolean firstLayerInspector) {
        this.firstLayerInspector = firstLayerInspector;
    }

    public String getHaltPrintSensitivity() {
        return haltPrintSensitivity;
    }

    public void setHaltPrintSensitivity(String haltPrintSensitivity) {
        this.haltPrintSensitivity = haltPrintSensitivity;
    }

    public boolean isPrintHalt() {
        return printHalt;
    }

    public void setPrintHalt(boolean printHalt) {
        this.printHalt = printHalt;
    }

    public boolean isPrintingMonitor() {
        return printingMonitor;
    }

    public void setPrintingMonitor(boolean printingMonitor) {
        this.printingMonitor = printingMonitor;
    }

    public boolean isSpaghettiDetector() {
        return spaghettiDetector;
    }

    public void setSpaghettiDetector(boolean spaghettiDetector) {
        this.spaghettiDetector = spaghettiDetector;
    }
}
