package com.czagrzebski.printhelm.web.controller;

import com.czagrzebski.printhelm.web.domain.connection.MQTTConnectionConfig;
import com.czagrzebski.printhelm.web.service.HlsStreamManager;
import com.czagrzebski.printhelm.web.service.PrinterService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api")
public class StreamingController {

    private static final Logger logger = LogManager.getLogger(StreamingController.class);
    private static final int CAMERA_PORT = 322;
    private static final String CAMERA_PATH = "/streaming/live/1";
    private static final int PLAYLIST_WAIT_MS = 500;
    private static final int PLAYLIST_MAX_ATTEMPTS = 20; // 10 seconds total

    private final PrinterService printerService;
    private final HlsStreamManager hlsStreamManager;

    public StreamingController(PrinterService printerService, HlsStreamManager hlsStreamManager) {
        this.printerService = printerService;
        this.hlsStreamManager = hlsStreamManager;
    }

    @GetMapping(value = "/printer/{id}/stream/index.m3u8", produces = "application/vnd.apple.mpegurl")
    public ResponseEntity<byte[]> streamPlaylist(@PathVariable long id) throws IOException, InterruptedException {
        Path dir = startStream(id);
        if (dir == null) return ResponseEntity.notFound().build();

        Path playlist = dir.resolve("index.m3u8");
        for (int i = 0; i < PLAYLIST_MAX_ATTEMPTS; i++) {
            if (Files.exists(playlist) && Files.size(playlist) > 0) break;
            Thread.sleep(PLAYLIST_WAIT_MS);
        }

        if (!Files.exists(playlist)) {
            return ResponseEntity.internalServerError().build();
        }

        hlsStreamManager.keepAlive(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .contentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"))
                .body(Files.readAllBytes(playlist));
    }

    @GetMapping(value = "/printer/{id}/stream/{file:.+\\.ts}", produces = "video/mp2t")
    public ResponseEntity<byte[]> streamSegment(@PathVariable long id, @PathVariable String file) throws IOException {
        if (!file.matches("[a-zA-Z0-9_-]+\\.ts")) {
            return ResponseEntity.badRequest().build();
        }

        Path dir = hlsStreamManager.getDir(id);
        if (dir == null) return ResponseEntity.notFound().build();

        Path segment = dir.resolve(file);
        if (!segment.startsWith(dir) || !Files.exists(segment)) {
            return ResponseEntity.notFound().build();
        }

        hlsStreamManager.keepAlive(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                .contentType(MediaType.parseMediaType("video/mp2t"))
                .body(Files.readAllBytes(segment));
    }

    private Path startStream(long id) throws IOException {
        var printer = printerService.getPrinterById(id);
        var config = printer.getConnectionConfig();

        if (!(config instanceof MQTTConnectionConfig mqttConfig)) return null;

        String ip = extractIp(mqttConfig.getBrokerUrl());
        if (ip == null || ip.isBlank()) return null;

        String rtspUrl = "rtsps://bblp:" + mqttConfig.getPassword()
                + "@" + ip + ":" + CAMERA_PORT + CAMERA_PATH;

        return hlsStreamManager.startOrKeepAlive(id, rtspUrl);
    }

    private String extractIp(String brokerUrl) {
        if (brokerUrl == null) return null;
        String withoutScheme = brokerUrl.replaceFirst("^[a-zA-Z]+://", "");
        int colonIdx = withoutScheme.lastIndexOf(':');
        return colonIdx > 0 ? withoutScheme.substring(0, colonIdx) : withoutScheme;
    }
}
