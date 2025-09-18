package com.czagrzebski.printhelm.web.domain.bambulab;

import com.czagrzebski.printhelm.model.PrinterType;
import com.czagrzebski.printhelm.web.domain.Printer;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Inheritance;

import java.util.HashMap;
import java.util.Map;

@Entity
@Inheritance(strategy = jakarta.persistence.InheritanceType.SINGLE_TABLE)
@DiscriminatorValue("BAMBULAB")
public class BambuLabPrinter extends Printer {

    public enum PrintStage {
        OFFLINE(-2, "Offline"),
        IDLE(-1, "Idle"),
        PRINTING(0, "Printing"),
        AUTO_BED_LEVELING(1, "Auto bed leveling"),
        HEATBED_PREHEATING(2, "Heatbed preheating"),
        SWEEPING_XY(3, "Sweeping XY mech mode"),
        CHANGING_FILAMENT(4, "Changing filament"),
        M400_PAUSE(5, "M400 pause"),
        FILAMENT_RUNOUT(6, "Paused due to filament runout"),
        HEATING_HOTEND(7, "Heating hotend"),
        CALIBRATING_EXTRUSION(8, "Calibrating extrusion"),
        SCANNING_BED(9, "Scanning bed surface"),
        INSPECTING_FIRST_LAYER(10, "Inspecting first layer"),
        IDENTIFYING_BUILD_PLATE(11, "Identifying build plate type"),
        CALIBRATING_MICRO_LIDAR(12, "Calibrating Micro Lidar"),
        HOMING_TOOLHEAD(13, "Homing toolhead"),
        CLEANING_NOZZLE(14, "Cleaning nozzle tip"),
        CHECKING_EXTRUDER_TEMP(15, "Checking extruder temperature"),
        PAUSED_BY_USER(16, "Printing was paused by the user"),
        FRONT_COVER_PAUSE(17, "Pause of front cover falling"),
        CALIBRATING_MICRO_LIDAR_2(18, "Calibrating the micro lidar"),
        CALIBRATING_EXTRUSION_FLOW(19, "Calibrating extrusion flow"),
        NOZZLE_TEMP_ERROR(20, "Paused due to nozzle temperature malfunction"),
        BED_TEMP_ERROR(21, "Paused due to heat bed temperature malfunction");

        private final int code;
        private final String description;

        private static final Map<Integer, PrintStage> LOOKUP = new HashMap<>();

        static {
            for (PrintStage stage : PrintStage.values()) {
                LOOKUP.put(stage.code, stage);
            }
        }

        PrintStage(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }

        public static PrintStage fromCode(int code) {
            return LOOKUP.getOrDefault(code, null);
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
