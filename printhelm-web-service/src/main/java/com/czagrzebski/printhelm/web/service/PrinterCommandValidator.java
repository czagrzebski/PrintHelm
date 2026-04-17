package com.czagrzebski.printhelm.web.service;

import java.util.Set;
import java.util.regex.Pattern;

public final class PrinterCommandValidator {

    private static final Set<String> ALLOWED_AXES = Set.of("X", "Y", "Z", "E");
    private static final double MAX_JOG_DISTANCE = 500.0;

    private static final Set<String> ALLOWED_LIGHT_NODES = Set.of(
            "chamber_light", "work_light"
    );
    private static final Set<String> ALLOWED_LIGHT_MODES = Set.of(
            "on", "off", "flashing"
    );

    // Filenames must only contain safe characters — no path separators or shell metacharacters
    private static final Pattern SAFE_FILENAME = Pattern.compile("^[\\w\\-. ()]+\\.(3mf|gcode)$", Pattern.CASE_INSENSITIVE);

    private PrinterCommandValidator() {}

    public static void validateAxis(String axis) {
        if (axis == null || !ALLOWED_AXES.contains(axis.toUpperCase())) {
            throw new IllegalArgumentException(
                    "Invalid axis '" + axis + "'. Must be one of: " + ALLOWED_AXES);
        }
    }

    public static void validateJogDistance(double distance) {
        if (distance == 0) {
            throw new IllegalArgumentException("Jog distance must be non-zero.");
        }
        if (Math.abs(distance) > MAX_JOG_DISTANCE) {
            throw new IllegalArgumentException(
                    "Jog distance " + distance + " mm exceeds the maximum of ±" + MAX_JOG_DISTANCE + " mm.");
        }
    }

    public static void validateSpeedLevel(int level) {
        if (level < 1 || level > 4) {
            throw new IllegalArgumentException(
                    "Speed level " + level + " is invalid. Must be 1 (silent), 2 (standard), 3 (sport), or 4 (ludicrous).");
        }
    }

    public static void validateLightNode(String node) {
        if (node == null || !ALLOWED_LIGHT_NODES.contains(node.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Invalid light node '" + node + "'. Must be one of: " + ALLOWED_LIGHT_NODES);
        }
    }

    public static void validateLightMode(String mode) {
        if (mode == null || !ALLOWED_LIGHT_MODES.contains(mode.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Invalid light mode '" + mode + "'. Must be one of: " + ALLOWED_LIGHT_MODES);
        }
    }

    public static void validateFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename must not be empty.");
        }
        if (!SAFE_FILENAME.matcher(filename).matches()) {
            throw new IllegalArgumentException(
                    "Filename '" + filename + "' contains invalid characters or extension. " +
                    "Only .3mf and .gcode files with alphanumeric names are accepted.");
        }
    }
}
