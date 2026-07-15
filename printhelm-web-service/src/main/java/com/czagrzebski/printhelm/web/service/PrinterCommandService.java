package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.web.connection.MqttConnectionManager;
import com.czagrzebski.printhelm.web.domain.connection.MQTTConnectionConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.stereotype.Service;

@Service
public class PrinterCommandService {

    private static final Logger logger = LogManager.getLogger(PrinterCommandService.class);

    private final PrinterService printerService;
    private final MqttConnectionManager mqttConnectionManager;
    private final AuditLogService auditLogService;

    public PrinterCommandService(PrinterService printerService, MqttConnectionManager mqttConnectionManager,
                                 AuditLogService auditLogService) {
        this.printerService = printerService;
        this.mqttConnectionManager = mqttConnectionManager;
        this.auditLogService = auditLogService;
    }

    public void stopPrint(long printerId) throws MqttException {
        String printerName = publish(printerId, printCommand("stop", ""), 1);
        auditLogService.record("PRINT_STOPPED", "Printer", printerId, "Stopped print on " + printerName);
    }

    public void pausePrint(long printerId) throws MqttException {
        String printerName = publish(printerId, printCommand("pause", ""), 1);
        auditLogService.record("PRINT_PAUSED", "Printer", printerId, "Paused print on " + printerName);
    }

    public void resumePrint(long printerId) throws MqttException {
        String printerName = publish(printerId, printCommand("resume", ""), 1);
        auditLogService.record("PRINT_RESUMED", "Printer", printerId, "Resumed print on " + printerName);
    }

    public void setSpeed(long printerId, int level) throws MqttException {
        PrinterCommandValidator.validateSpeedLevel(level);
        String printerName = publish(printerId, printCommand("print_speed", String.valueOf(level)), 0);
        auditLogService.record("PRINT_SPEED_SET", "Printer", printerId,
                "Set print speed level to " + level + " on " + printerName);
    }

    public void jog(long printerId, String axis, double distance) throws MqttException {
        PrinterCommandValidator.validateAxis(axis);
        PrinterCommandValidator.validateJogDistance(distance);
        String gcode = "G91\nG0 " + axis.toUpperCase() + distance + " F3000\nG90\n";
        String printerName = publish(printerId, printCommand("gcode_line", gcode), 0);
        auditLogService.record("AXIS_JOGGED", "Printer", printerId,
                "Jogged " + axis.toUpperCase() + " axis " + distance + "mm on " + printerName);
    }

    public void home(long printerId) throws MqttException {
        String printerName = publish(printerId, printCommand("gcode_line", "G28\n"), 0);
        auditLogService.record("AXES_HOMED", "Printer", printerId, "Homed axes on " + printerName);
    }

    public void printFile(long printerId, String filename, int[] amsMapping, boolean flowCali, boolean vibrationCali, boolean layerInspect) throws MqttException {
        PrinterCommandValidator.validateFilename(filename);
        boolean useAms = amsMapping != null && amsMapping.length > 0;

        // ams_mapping must be a JSON array, not a quoted string
        StringBuilder amsMappingJson = new StringBuilder("[");
        if (useAms) {
            for (int i = 0; i < amsMapping.length; i++) {
                if (i > 0) amsMappingJson.append(",");
                amsMappingJson.append(amsMapping[i]);
            }
        }
        amsMappingJson.append("]");

        // .3mf files need the internal plate gcode path; .gcode files are addressed directly
        String param = filename.toLowerCase().endsWith(".3mf") ? "Metadata/plate_1.gcode" : filename;
        String url = "file:///mnt/sdcard/" + filename;

        String payload = String.format(
                "{\"print\":{\"sequence_id\":\"0\",\"command\":\"project_file\"," +
                "\"param\":\"%s\",\"url\":\"%s\",\"project_id\":\"0\"," +
                "\"profile_id\":\"0\",\"task_id\":\"0\",\"subtask_id\":\"0\",\"subtask_name\":\"\"," +
                "\"file\":\"\",\"md5\":\"\",\"timelapse\":false,\"bed_type\":\"auto\"," +
                "\"bed_levelling\":true,\"flow_cali\":%b,\"vibration_cali\":%b," +
                "\"layer_inspect\":%b,\"use_ams\":%b,\"ams_mapping\":%s}}",
                escape(param), escape(url), flowCali, vibrationCali, layerInspect, useAms, amsMappingJson);
        String printerName = publish(printerId, payload, 1);
        auditLogService.record("PRINT_STARTED", "Printer", printerId,
                "Started print of " + filename + " on " + printerName);
    }

    public void setNozzleTemp(long printerId, int temp) throws MqttException {
        PrinterCommandValidator.validateNozzleTemp(temp);
        String printerName = publish(printerId, printCommand("gcode_line", "M104 S" + temp + "\n"), 0);
        auditLogService.record("NOZZLE_TEMP_SET", "Printer", printerId,
                "Set nozzle temperature to " + temp + "°C on " + printerName);
    }

    public void setBedTemp(long printerId, int temp) throws MqttException {
        PrinterCommandValidator.validateBedTemp(temp);
        String printerName = publish(printerId, printCommand("gcode_line", "M140 S" + temp + "\n"), 0);
        auditLogService.record("BED_TEMP_SET", "Printer", printerId,
                "Set bed temperature to " + temp + "°C on " + printerName);
    }

    public void setLight(long printerId, String ledNode, String mode) throws MqttException {
        PrinterCommandValidator.validateLightNode(ledNode);
        PrinterCommandValidator.validateLightMode(mode);
        String payload = "{\"system\":{\"sequence_id\":\"0\",\"command\":\"ledctrl\","
                + "\"led_node\":\"" + escape(ledNode) + "\","
                + "\"led_mode\":\"" + escape(mode) + "\","
                + "\"led_on_time\":500,\"led_off_time\":500,\"loop_times\":1,\"interval_time\":1000}}";
        String printerName = publish(printerId, payload, 0);
        auditLogService.record("LIGHT_SET", "Printer", printerId,
                "Set " + ledNode + " to " + mode + " on " + printerName);
    }

    private String printCommand(String command, String param) {
        return "{\"print\":{\"sequence_id\":\"0\",\"command\":\"" + command + "\",\"param\":\"" + escape(param) + "\"}}";
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }

    /** Publishes the payload and returns the printer's name for audit details. */
    private String publish(long printerId, String payload, int qos) throws MqttException {
        var printer = printerService.getPrinterById(printerId);
        if (!(printer.getConnectionConfig() instanceof MQTTConnectionConfig mqttConfig)) {
            throw new IllegalArgumentException("Printer [ID=" + printerId + "] has no MQTT config");
        }
        String topic = mqttConfig.getTopic() + "/request";
        logger.info("Sending command to printer [ID={}]: {}", printerId, payload);
        mqttConnectionManager.publish(printerId, topic, payload, qos);
        return printer.getPrinterName();
    }
}
