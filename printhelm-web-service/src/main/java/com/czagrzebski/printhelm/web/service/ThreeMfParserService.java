package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.web.domain.GcodeFilamentInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ThreeMfParserService {

    private final ObjectMapper objectMapper;

    public ThreeMfParserService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedThreeMfData parse(InputStream stream) throws IOException {
        // ZipArchiveInputStream (sequential) fails on entries that use data descriptors.
        // Write to a temp file so ZipFile can use random access via the central directory.
        Path tmp = Files.createTempFile("3mf-", ".zip");
        try {
            Files.copy(stream, tmp, StandardCopyOption.REPLACE_EXISTING);
            return parseZipFile(tmp);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    private ParsedThreeMfData parseZipFile(Path path) throws IOException {
        byte[] settingsBytes = null;
        byte[] sequenceBytes = null;

        try (ZipFile zip = ZipFile.builder().setPath(path).get()) {
            Enumeration<ZipArchiveEntry> entries = zip.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.equals("Metadata/project_settings.config")) {
                    try (InputStream in = zip.getInputStream(entry)) {
                        settingsBytes = readStream(in);
                    }
                } else if (name.endsWith("filament_sequence.json")) {
                    try (InputStream in = zip.getInputStream(entry)) {
                        sequenceBytes = readStream(in);
                    }
                }
            }
        }

        if (settingsBytes == null) {
            return ParsedThreeMfData.empty();
        }

        JsonNode settings = objectMapper.readTree(settingsBytes);

        List<String> colours = jsonStringArray(settings, "filament_colour");
        List<String> types = jsonStringArray(settings, "filament_type");

        List<Integer> activeSlots = resolveActiveSlots(sequenceBytes, colours.size());

        List<GcodeFilamentInfo> filaments = new ArrayList<>();
        for (int slotIndex : activeSlots) {
            String color = slotIndex < colours.size() ? colours.get(slotIndex) : "#FFFFFF";
            String type = slotIndex < types.size() ? types.get(slotIndex) : "Unknown";
            filaments.add(new GcodeFilamentInfo(slotIndex, type, color));
        }

        return new ParsedThreeMfData(filaments, filaments.size() > 1, filaments.size());
    }

    private List<Integer> resolveActiveSlots(byte[] sequenceBytes, int totalSlots) throws IOException {
        if (sequenceBytes != null) {
            JsonNode seqRoot = objectMapper.readTree(sequenceBytes);
            JsonNode plate = seqRoot.path("plate_1").path("sequence");
            if (plate.isArray() && !plate.isEmpty()) {
                Set<Integer> seen = new LinkedHashSet<>();
                for (JsonNode n : plate) {
                    // Bambu Lab sequence indices are 1-based; convert to 0-based slot index
                    int oneBased = n.asInt();
                    seen.add(oneBased - 1);
                }
                return new ArrayList<>(seen);
            }
        }
        // No sequence info — treat every configured slot as active
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < totalSlots; i++) all.add(i);
        return all;
    }

    private List<String> jsonStringArray(JsonNode node, String field) {
        List<String> result = new ArrayList<>();
        JsonNode arr = node.path(field);
        if (arr.isArray()) {
            for (JsonNode n : arr) result.add(n.asText());
        }
        return result;
    }

    private byte[] readStream(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read;
        while ((read = in.read(chunk)) != -1) buf.write(chunk, 0, read);
        return buf.toByteArray();
    }

    public static class ParsedThreeMfData {
        private final List<GcodeFilamentInfo> filaments;
        private final boolean multiColor;
        private final int colorCount;

        public ParsedThreeMfData(List<GcodeFilamentInfo> filaments, boolean multiColor, int colorCount) {
            this.filaments = filaments;
            this.multiColor = multiColor;
            this.colorCount = colorCount;
        }

        public static ParsedThreeMfData empty() {
            return new ParsedThreeMfData(List.of(), false, 0);
        }

        public List<GcodeFilamentInfo> getFilaments() { return filaments; }
        public boolean isMultiColor() { return multiColor; }
        public int getColorCount() { return colorCount; }
    }
}
