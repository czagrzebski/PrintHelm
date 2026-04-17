package com.czagrzebski.printhelm.web.controller;

import com.czagrzebski.printhelm.web.domain.connection.MQTTConnectionConfig;
import com.czagrzebski.printhelm.web.service.PrinterService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api")
public class StreamingController {

    private static final Logger logger = LogManager.getLogger(StreamingController.class);
    private static final int CAMERA_PORT = 322;
    private static final String CAMERA_PATH = "/streaming/live/1";
    private static final String BOUNDARY = "frame";

    private final PrinterService printerService;

    public StreamingController(PrinterService printerService) {
        this.printerService = printerService;
    }

    @GetMapping("/printer/{id}/stream")
    public ResponseEntity<StreamingResponseBody> streamCamera(@PathVariable long id) {
        var printer = printerService.getPrinterById(id);
        var config = printer.getConnectionConfig();

        if (!(config instanceof MQTTConnectionConfig mqttConfig)) {
            return ResponseEntity.notFound().build();
        }

        String ip = extractIp(mqttConfig.getBrokerUrl());
        if (ip == null || ip.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        String rtspUrl = "rtsps://bblp:" + mqttConfig.getPassword()
                + "@" + ip + ":" + CAMERA_PORT + CAMERA_PATH;

        StreamingResponseBody body = outputStream -> {
            // image2pipe outputs raw concatenated JPEG files; we frame them manually
            // so we control the boundary and avoid mpjpeg's embedded Content-Type header.
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-loglevel", "error",
                    "-rtsp_transport", "tcp",
                    "-i", rtspUrl,
                    "-f", "image2pipe",
                    "-vcodec", "mjpeg",
                    "-q:v", "5",
                    "-r", "10",
                    "pipe:1"
            );
            pb.redirectError(ProcessBuilder.Redirect.INHERIT);

            Process process = pb.start();
            logger.info("Started camera stream for printer [ID={}]", id);

            try (InputStream is = process.getInputStream()) {
                pipeJpegFrames(is, outputStream);
            } catch (Exception e) {
                logger.debug("Camera stream ended for printer [ID={}]: {}", id, e.getMessage());
            } finally {
                process.destroyForcibly();
                logger.info("Stopped camera stream for printer [ID={}]", id);
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("multipart/x-mixed-replace;boundary=" + BOUNDARY))
                .body(body);
    }

    /**
     * Reads raw concatenated JPEGs from image2pipe and wraps each frame in
     * multipart/x-mixed-replace framing so browsers can display it as MJPEG.
     * JPEG frames start with 0xFF 0xD8 and end with 0xFF 0xD9.
     */
    private void pipeJpegFrames(InputStream is, OutputStream out) throws IOException {
        byte[] buf = new byte[65536];
        // Accumulate bytes; flush a frame each time we see FF D9
        java.io.ByteArrayOutputStream frame = new java.io.ByteArrayOutputStream(65536);
        int prev = -1;
        int b;

        while ((b = is.read()) != -1) {
            // New JPEG starts: reset accumulator (handles any garbage between frames)
            if (prev == 0xFF && b == 0xD8) {
                frame.reset();
                frame.write(0xFF);
            }
            frame.write(b);

            // End of JPEG: flush the accumulated frame
            if (prev == 0xFF && b == 0xD9 && frame.size() > 2) {
                byte[] jpeg = frame.toByteArray();
                String header = "--" + BOUNDARY + "\r\n"
                        + "Content-Type: image/jpeg\r\n"
                        + "Content-Length: " + jpeg.length + "\r\n"
                        + "\r\n";
                out.write(header.getBytes(StandardCharsets.US_ASCII));
                out.write(jpeg);
                out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
                out.flush();
                frame.reset();
            }

            prev = b;
        }
    }

    private String extractIp(String brokerUrl) {
        if (brokerUrl == null) return null;
        // brokerUrl format: ssl://192.168.1.1:8883 or tcp://192.168.1.1:1883
        String withoutScheme = brokerUrl.replaceFirst("^[a-zA-Z]+://", "");
        int colonIdx = withoutScheme.lastIndexOf(':');
        return colonIdx > 0 ? withoutScheme.substring(0, colonIdx) : withoutScheme;
    }
}
