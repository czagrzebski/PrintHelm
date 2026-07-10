package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.web.domain.GcodeFilamentInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
        byte[] sliceInfoBytes = null;

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
                } else if (name.equals("Metadata/slice_info.config")) {
                    try (InputStream in = zip.getInputStream(entry)) {
                        sliceInfoBytes = readStream(in);
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

        SliceInfo sliceInfo = parseSliceInfo(sliceInfoBytes);
        for (GcodeFilamentInfo filament : filaments) {
            Double grams = sliceInfo.usedGramsBySlot.get(filament.getSlotIndex());
            if (grams != null) filament.setUsedGrams(grams);
        }

        return new ParsedThreeMfData(filaments, filaments.size() > 1, filaments.size(),
                sliceInfo.predictionSeconds, sliceInfo.totalWeightGrams);
    }

    /**
     * Reads per-filament usage (used_g) and the print time prediction out of
     * Metadata/slice_info.config. Filament ids there are 1-based; slot indices are 0-based.
     * Only the first plate is considered, matching the filament_sequence handling above.
     */
    private SliceInfo parseSliceInfo(byte[] sliceInfoBytes) {
        SliceInfo result = new SliceInfo();
        if (sliceInfoBytes == null) return result;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(sliceInfoBytes));

            NodeList plates = doc.getElementsByTagName("plate");
            if (plates.getLength() == 0) return result;
            Element plate = (Element) plates.item(0);

            NodeList metadataNodes = plate.getElementsByTagName("metadata");
            for (int i = 0; i < metadataNodes.getLength(); i++) {
                Element meta = (Element) metadataNodes.item(i);
                String key = meta.getAttribute("key");
                String value = meta.getAttribute("value");
                if ("prediction".equals(key)) {
                    result.predictionSeconds = parseIntOrNull(value);
                } else if ("weight".equals(key)) {
                    result.totalWeightGrams = parseDoubleOrNull(value);
                }
            }

            NodeList filamentNodes = plate.getElementsByTagName("filament");
            for (int i = 0; i < filamentNodes.getLength(); i++) {
                Element filament = (Element) filamentNodes.item(i);
                Integer id = parseIntOrNull(filament.getAttribute("id"));
                Double usedGrams = parseDoubleOrNull(filament.getAttribute("used_g"));
                if (id != null && usedGrams != null) {
                    result.usedGramsBySlot.put(id - 1, usedGrams);
                }
            }
        } catch (Exception e) {
            // slice_info is best-effort enrichment; a malformed file shouldn't fail the upload
        }
        return result;
    }

    private Integer parseIntOrNull(String value) {
        try {
            return value == null || value.isBlank() ? null : (int) Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDoubleOrNull(String value) {
        try {
            return value == null || value.isBlank() ? null : Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static class SliceInfo {
        Map<Integer, Double> usedGramsBySlot = new HashMap<>();
        Integer predictionSeconds;
        Double totalWeightGrams;
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
        private final Integer estimatedDurationSeconds;
        private final Double totalWeightGrams;

        public ParsedThreeMfData(List<GcodeFilamentInfo> filaments, boolean multiColor, int colorCount,
                                 Integer estimatedDurationSeconds, Double totalWeightGrams) {
            this.filaments = filaments;
            this.multiColor = multiColor;
            this.colorCount = colorCount;
            this.estimatedDurationSeconds = estimatedDurationSeconds;
            this.totalWeightGrams = totalWeightGrams;
        }

        public static ParsedThreeMfData empty() {
            return new ParsedThreeMfData(List.of(), false, 0, null, null);
        }

        public List<GcodeFilamentInfo> getFilaments() { return filaments; }
        public boolean isMultiColor() { return multiColor; }
        public int getColorCount() { return colorCount; }
        public Integer getEstimatedDurationSeconds() { return estimatedDurationSeconds; }
        public Double getTotalWeightGrams() { return totalWeightGrams; }
    }
}
