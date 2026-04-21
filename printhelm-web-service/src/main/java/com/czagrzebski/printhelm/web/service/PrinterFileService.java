package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.web.domain.connection.MQTTConnectionConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**

 * Bambu printers require FTPS data connections to reuse the TLS session from the control connection.
 *
 * Apache Commons Net (Java FTPSClient) does not reliably support this, causing uploads to fail:
 * the file gets created but no data is transferred (connection reset after STOR).
 *
 * curl is used for uploads because it correctly handles FTPS session reuse and works consistently.
 */

@Service
public class PrinterFileService {

    private static final Logger logger = LogManager.getLogger(PrinterFileService.class);

    private static final int FTPS_PORT = 990;
    private static final String FTPS_USER = "bblp";

    public record PrinterFileDTO(String name, long sizeBytes, Instant lastModified) {}

    private final PrinterService printerService;

    public PrinterFileService(PrinterService printerService) {
        this.printerService = printerService;
    }

    public List<PrinterFileDTO> listFiles(long printerId) throws IOException {
        var config = resolveConfig(printerId);
        List<String> cmd = List.of(
                "curl", "-k",
                "-u", FTPS_USER + ":" + config.password(),
                ftpsUrl(config.host(), "/")
        );
        String output = runCurl(cmd, "listFiles", printerId);
        return parseListOutput(output);
    }

    public void uploadFile(long printerId, String filename, InputStream data) throws IOException {
        var config = resolveConfig(printerId);
        Path tmp = Files.createTempFile("printhelm-upload-", "-" + filename);
        try {
            Files.copy(data, tmp, StandardCopyOption.REPLACE_EXISTING);
            List<String> cmd = List.of(
                    "curl", "-k",
                    "-u", FTPS_USER + ":" + config.password(),
                    "-T", tmp.toString(),
                    ftpsUrl(config.host(), "/" + filename)
            );
            runCurl(cmd, "uploadFile:" + filename, printerId);
            logger.info("Uploaded {} to printer [ID={}]", filename, printerId);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    public byte[] downloadFile(long printerId, String filename) throws IOException {
        var config = resolveConfig(printerId);
        Path tmp = Files.createTempFile("printhelm-dl-", null);
        try {
            List<String> cmd = List.of(
                    "curl", "-k",
                    "-u", FTPS_USER + ":" + config.password(),
                    "--silent", "--show-error",
                    "-o", tmp.toString(),
                    ftpsUrl(config.host(), "/" + filename)
            );
            runCurl(cmd, "downloadFile:" + filename, printerId);
            return Files.readAllBytes(tmp);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    public void deleteFile(long printerId, String filename) throws IOException {
        var config = resolveConfig(printerId);
        List<String> cmd = List.of(
                "curl", "-k",
                "-u", FTPS_USER + ":" + config.password(),
                "-Q", "DELE " + filename,
                ftpsUrl(config.host(), "/")
        );
        runCurl(cmd, "deleteFile:" + filename, printerId);
        logger.info("Deleted {} from printer [ID={}]", filename, printerId);
    }

    private record FtpsConfig(String host, String password) {}

    private FtpsConfig resolveConfig(long printerId) throws IOException {
        var printer = printerService.getPrinterById(printerId);
        if (!(printer.getConnectionConfig() instanceof MQTTConnectionConfig mqttConfig)) {
            throw new IllegalArgumentException("Printer [ID=" + printerId + "] has no MQTT config");
        }
        return new FtpsConfig(extractHost(mqttConfig.getBrokerUrl()), mqttConfig.getPassword());
    }

    private static String ftpsUrl(String host, String path) {
        return "ftps://" + host + ":" + FTPS_PORT + path;
    }

    private String runCurl(List<String> cmd, String operation, long printerId) throws IOException {
        Process process = new ProcessBuilder(cmd).start();

        StringBuilder stderrBuf = new StringBuilder();
        Thread stderrReader = new Thread(() -> {
            try {
                stderrBuf.append(new String(process.getErrorStream().readAllBytes()));
            } catch (IOException ignored) {}
        });
        stderrReader.start();

        String stdout = new String(process.getInputStream().readAllBytes());

        try {
            stderrReader.join();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                String stderr = stderrBuf.toString().trim();
                logger.error("curl {} failed for printer [ID={}]: exit={}, stderr={}", operation, printerId, exitCode, stderr);
                throw new IOException("FTPS " + operation + " failed (curl exit " + exitCode + "): " + stderr);
            }
            return stdout;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("Interrupted during curl " + operation, e);
        }
    }

    private List<PrinterFileDTO> parseListOutput(String listing) {
        List<PrinterFileDTO> files = new ArrayList<>();
        for (String line : listing.split("\n")) {
            line = line.strip();
            if (line.isEmpty() || !line.startsWith("-")) continue;
            // Standard Unix LIST format: perms links owner group size month day time/year name
            String[] parts = line.split("\\s+", 9);
            if (parts.length < 9) continue;
            try {
                long size = Long.parseLong(parts[4]);
                String name = parts[8].strip();
                if (name.startsWith(".")) continue;
                Instant lastModified = parseListTimestamp(parts[5], parts[6], parts[7]);
                files.add(new PrinterFileDTO(name, size, lastModified));
            } catch (Exception e) {
                logger.warn("Could not parse FTP LIST line: {}", line);
            }
        }
        return files;
    }

    // Parses "Jan 1 12:00" (recent file) or "Jan 1 2023" (older file) from FTP LIST output
    private static Instant parseListTimestamp(String month, String day, String timeOrYear) {
        int year;
        int hour = 0, minute = 0;
        if (timeOrYear.contains(":")) {
            String[] tp = timeOrYear.split(":");
            hour = Integer.parseInt(tp[0]);
            minute = Integer.parseInt(tp[1]);
            year = LocalDateTime.now(ZoneOffset.UTC).getYear();
        } else {
            year = Integer.parseInt(timeOrYear);
        }
        String dateStr = String.format("%s %s %d %02d:%02d", month, day.strip(), year, hour, minute);
        return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("MMM d yyyy HH:mm", Locale.ENGLISH))
                .toInstant(ZoneOffset.UTC);
    }

    private String extractHost(String brokerUrl) throws IOException {
        try {
            return new URI(brokerUrl).getHost();
        } catch (Exception e) {
            throw new IOException("Could not parse printer host from broker URL: " + brokerUrl, e);
        }
    }
}
