package com.czagrzebski.printhelm.web.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Decodes Bambu Lab error codes from a bundled JSON database sourced from ha-bambulab.
 * To update: re-download hms_en.json.gz from greghesp/ha-bambulab, re-run the conversion
 * script, and replace bambulab-error-codes.json.gz in src/main/resources.
 */
@Component
public class BambuLabErrorRegistry {

    private static final Logger log = LogManager.getLogger(BambuLabErrorRegistry.class);

    private Map<String, HmsEntry> hmsErrors = Collections.emptyMap();
    private Map<String, String> printErrors = Collections.emptyMap();

    @JsonIgnoreProperties(ignoreUnknown = true)
    record HmsEntry(String description, String severity) {}

    @SuppressWarnings("unchecked")
    @PostConstruct
    void load() {
        try (InputStream raw = new ClassPathResource("bambulab-error-codes.json.gz").getInputStream();
             GZIPInputStream gz = new GZIPInputStream(raw)) {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> root = mapper.readValue(gz, Map.class);
            hmsErrors = mapper.convertValue(root.get("hmsErrors"),
                    mapper.getTypeFactory().constructMapType(Map.class, String.class, HmsEntry.class));
            printErrors = mapper.convertValue(root.get("printErrors"),
                    mapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
            log.info("BambuLabErrorRegistry loaded: {} HMS codes, {} print error codes",
                    hmsErrors.size(), printErrors.size());
        } catch (Exception e) {
            log.error("Failed to load bambulab-error-codes.json.gz — error decoding will be unavailable", e);
        }
    }

    /**
     * Decodes a printError integer (from ApiPrinterState.printError).
     * Returns null if code is 0 (no error) or unknown.
     */
    public String decodePrintError(int code) {
        if (code == 0) return null;
        return printErrors.get(String.valueOf(code));
    }

    /**
     * Decodes an HMS error from the raw attr+code integers reported in the MQTT hms list.
     * Falls back to a generic description if the exact code is not in the database.
     */
    public String decodeHmsError(int attr, int code) {
        String key = String.format("%08x%08x", attr, code);
        HmsEntry entry = hmsErrors.get(key);
        if (entry != null) return entry.description();
        // Unknown code — at minimum report the hex value so Claude has something to work with
        return String.format("Unknown hardware error (attr=0x%08X, code=0x%08X)", attr, code);
    }

    /**
     * Parses the raw List&lt;Object&gt; from PrintDTO.hms into decoded human-readable descriptions.
     * Each element is expected to be a Map with "attr" and "code" integer keys.
     */
    @SuppressWarnings("unchecked")
    public List<String> decodeHmsList(List<Object> hms) {
        if (hms == null || hms.isEmpty()) return Collections.emptyList();
        return hms.stream()
                .filter(o -> o instanceof Map)
                .map(o -> (Map<String, Object>) o)
                .filter(m -> m.containsKey("attr") && m.containsKey("code"))
                .map(m -> {
                    int attr = ((Number) m.get("attr")).intValue();
                    int code = ((Number) m.get("code")).intValue();
                    return decodeHmsError(attr, code);
                })
                .toList();
    }
}
