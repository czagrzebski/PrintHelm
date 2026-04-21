package com.czagrzebski.printhelm.web.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Service
public class HlsStreamManager {

    private static final Logger logger = LogManager.getLogger(HlsStreamManager.class);
    private static final long IDLE_TIMEOUT_MS = 30_000;
    private static final Path BASE_DIR = Path.of(System.getProperty("java.io.tmpdir"), "printhelm", "streams");

    private static class HlsSession {
        final Process process;
        final Path dir;
        final AtomicLong lastAccessMs = new AtomicLong(System.currentTimeMillis());

        HlsSession(Process process, Path dir) {
            this.process = process;
            this.dir = dir;
        }

        void touch() {
            lastAccessMs.set(System.currentTimeMillis());
        }
    }

    private final ConcurrentHashMap<Long, HlsSession> sessions = new ConcurrentHashMap<>();

    public synchronized Path startOrKeepAlive(long printerId, String rtspUrl) throws IOException {
        HlsSession existing = sessions.get(printerId);
        if (existing != null && existing.process.isAlive()) {
            existing.touch();
            return existing.dir;
        }

        Path dir = BASE_DIR.resolve(String.valueOf(printerId));
        Files.createDirectories(dir);

        // Remove stale segments from a previous session
        try (Stream<Path> files = Files.list(dir)) {
            files.forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        }

        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg",
                "-loglevel", "error",
                "-rtsp_transport", "tcp",
                "-i", rtspUrl,
                "-c:v", "copy",
                "-an",
                "-f", "hls",
                "-hls_time", "2",
                "-hls_list_size", "5",
                "-hls_flags", "delete_segments+append_list",
                "-hls_segment_filename", dir.resolve("seg%03d.ts").toString(),
                dir.resolve("index.m3u8").toString()
        );
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);
        Process process = pb.start();

        HlsSession session = new HlsSession(process, dir);
        sessions.put(printerId, session);
        logger.info("Started HLS stream for printer [ID={}]", printerId);
        return dir;
    }

    public Path getDir(long printerId) {
        HlsSession s = sessions.get(printerId);
        return s != null ? s.dir : null;
    }

    public void keepAlive(long printerId) {
        HlsSession s = sessions.get(printerId);
        if (s != null) s.touch();
    }

    @Scheduled(fixedDelay = 10_000)
    public void cleanup() {
        long now = System.currentTimeMillis();
        sessions.forEach((printerId, session) -> {
            boolean timedOut = now - session.lastAccessMs.get() > IDLE_TIMEOUT_MS;
            boolean dead = !session.process.isAlive();
            if (timedOut || dead) {
                sessions.remove(printerId);
                session.process.destroyForcibly();
                deleteDir(session.dir);
                logger.info("Stopped HLS stream for printer [ID={}] ({})", printerId, timedOut ? "idle timeout" : "process dead");
            }
        });
    }

    private void deleteDir(Path dir) {
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder())
                .forEach(p -> { try { Files.deleteIfExists(p); } catch (IOException ignored) {} });
        } catch (IOException ignored) {}
    }
}
