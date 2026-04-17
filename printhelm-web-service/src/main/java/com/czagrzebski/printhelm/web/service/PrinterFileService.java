package com.czagrzebski.printhelm.web.service;

import com.czagrzebski.printhelm.web.domain.connection.MQTTConnectionConfig;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.util.TrustManagerUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSessionContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.security.*;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
public class PrinterFileService {

    private static final Logger logger = LogManager.getLogger(PrinterFileService.class);
    private static final int FTPS_PORT = 990;

    public record PrinterFileDTO(String name, long sizeBytes, Instant lastModified) {}

    private final PrinterService printerService;

    public PrinterFileService(PrinterService printerService) {
        this.printerService = printerService;
    }

    public List<PrinterFileDTO> listFiles(long printerId) throws IOException {
        FTPSClient client = createClient(printerId);
        try {
            FTPFile[] ftpFiles = client.listFiles();
            return Arrays.stream(ftpFiles)
                    .filter(f -> f.isFile() && !f.getName().startsWith("."))
                    .map(f -> new PrinterFileDTO(f.getName(), f.getSize(), f.getTimestampInstant()))
                    .toList();
        } catch (IOException e) {
            logFtpError("listFiles", printerId, e, client);
            throw e;
        } finally {
            disconnect(client, printerId);
        }
    }

    public void uploadFile(long printerId, String filename, InputStream data) throws IOException {
        FTPSClient client = createClient(printerId);
        try {
            boolean success = client.storeFile(filename, data);
            if (!success) {
                throw new IOException("FTP upload failed for " + filename + ": " + client.getReplyString());
            }
            logger.info("Uploaded {} to printer [ID={}]", filename, printerId);
        } catch (IOException e) {
            logFtpError("uploadFile:" + filename, printerId, e, client);
            throw e;
        } finally {
            disconnect(client, printerId);
        }
    }

    public void deleteFile(long printerId, String filename) throws IOException {
        FTPSClient client = createClient(printerId);
        try {
            boolean success = client.deleteFile(filename);
            if (!success) {
                throw new IOException("FTP delete failed for " + filename + ": " + client.getReplyString());
            }
            logger.info("Deleted {} from printer [ID={}]", filename, printerId);
        } catch (IOException e) {
            logFtpError("deleteFile:" + filename, printerId, e, client);
            throw e;
        } finally {
            disconnect(client, printerId);
        }
    }

    private void logFtpError(String op, long printerId, IOException e, FTPSClient client) {
        logger.error("FTPS {} failed for printer [ID={}] (last reply: {}): {}",
                op, printerId, client.getReplyString().trim(), e.getMessage());
        for (Throwable t = e.getCause(); t != null; t = t.getCause()) {
            logger.error("  Caused by [{}]: {}", t.getClass().getName(), t.getMessage());
        }
    }

    private FTPSClient createClient(long printerId) throws IOException {
        var printer = printerService.getPrinterById(printerId);
        if (!(printer.getConnectionConfig() instanceof MQTTConnectionConfig mqttConfig)) {
            throw new IllegalArgumentException("Printer [ID=" + printerId + "] has no MQTT config");
        }

        String ip;
        try {
            ip = new URI(mqttConfig.getBrokerUrl()).getHost();
        } catch (Exception e) {
            throw new IOException("Could not parse printer IP from broker URL", e);
        }

        FTPSClient client;
        try {
            client = buildFtpsClient();
        } catch (GeneralSecurityException e) {
            throw new IOException("Failed to create FTPS client", e);
        }

        client.setConnectTimeout(10_000);
        client.setDefaultTimeout(30_000);
        client.connect(ip, FTPS_PORT);
        client.setSoTimeout(30_000);
        client.setDataTimeout(Duration.ofSeconds(30));

        if (!client.login("bblp", mqttConfig.getPassword())) {
            client.disconnect();
            throw new IOException("FTPS login failed for printer [ID=" + printerId + "]");
        }

        // PBSZ/PROT must come after login — BambuLab rejects them pre-auth
        client.execPBSZ(0);
        client.execPROT("P");
        client.enterLocalPassiveMode();
        client.setFileType(FTP.BINARY_FILE_TYPE);
        logger.debug("FTPS connected to printer [ID={}] at {}:{}", printerId, ip, FTPS_PORT);
        return client;
    }

    /**
     * BambuLab requires TLS session reuse on the data channel (RFC 4217 §9).
     * Java's TLS session cache is keyed by host:port. The data channel uses a random
     * passive port so it never hits the session cached for the control port 990.
     *
     * Fix: wrap the SSLContext in a delegating SPI that returns a custom SSLSocketFactory.
     * That factory substitutes FTPS_PORT (990) for any data-channel port so the cache
     * lookup finds the control-channel session and resumes it.
     *
     * ctx.getSocketFactory() is guaranteed to be called by FTPSClient for both control
     * and data channels, so the interception is reliable regardless of FTPSClient version.
     */
    private static FTPSClient buildFtpsClient() throws GeneralSecurityException {
        // Disable TLS session tickets so Java is forced to use session IDs for resumption.
        // mbedTLS on BambuLab printers requires session-ID-based resumption on the data channel.
        System.setProperty("jdk.tls.client.enableSessionTicketExtension", "false");

        // Use "TLS" (not "TLSv1.2") so the context's session cache stores sessions normally;
        // TLS 1.2 is still negotiated because that's the highest the printer supports.
        SSLContext real = SSLContext.getInstance("TLS");
        real.init(null, new TrustManager[]{TrustManagerUtils.getAcceptAllTrustManager()}, null);

        SSLSocketFactory sessionReuseFactory = new ControlPortSocketFactory(real.getSocketFactory());

        SSLContext ctx = new SSLContext(
                new DelegatingSSLContextSpi(real, sessionReuseFactory),
                real.getProvider(),
                "TLS") {};

        return new FTPSClient(true, ctx);
    }

    /**
     * Substitutes FTPS_PORT for the data-channel port in createSocket(Socket,...) so Java
     * finds the session cached under the control port. All other overloads pass through.
     */
    private static final class ControlPortSocketFactory extends SSLSocketFactory {
        private final SSLSocketFactory delegate;
        private volatile byte[] controlSessionId;

        ControlPortSocketFactory(SSLSocketFactory delegate) {
            this.delegate = delegate;
        }

        @Override
        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            SSLSocket socket = (SSLSocket) delegate.createSocket(s, host, FTPS_PORT, autoClose);
            socket.setEnabledProtocols(new String[]{"TLSv1.2"});
            final byte[] capturedControl = controlSessionId;
            socket.addHandshakeCompletedListener(event -> {
                byte[] id = event.getSession().getId();
                String proto = event.getSession().getProtocol();
                if (capturedControl == null) {
                    controlSessionId = id;
                    logger.info("Control TLS: protocol={} sessionId.len={} hex={}", proto, id.length, hex(id));
                } else {
                    boolean resumed = java.util.Arrays.equals(capturedControl, id) && id.length > 0;
                    logger.info("Data TLS: protocol={} sessionId.len={} hex={} resumed={}", proto, id.length, hex(id), resumed);
                }
            });
            if (capturedControl != null) {
                // Data channel: start handshake explicitly to surface TLS errors clearly
                try {
                    socket.startHandshake();
                } catch (IOException e) {
                    logger.error("Data channel TLS handshake failed: {}", e.getMessage(), e);
                    throw e;
                }
                SSLSession sess = socket.getSession();
                boolean resumed = java.util.Arrays.equals(capturedControl, sess.getId()) && sess.getId().length > 0;
                logger.info("Data channel ready: protocol={} cipher={} resumed={}", sess.getProtocol(), sess.getCipherSuite(), resumed);
            }
            return socket;
        }

        private static String hex(byte[] b) {
            if (b == null || b.length == 0) return "(empty)";
            var sb = new StringBuilder();
            for (byte v : b) sb.append(String.format("%02x", v));
            return sb.toString();
        }

        @Override public String[] getDefaultCipherSuites() { return delegate.getDefaultCipherSuites(); }
        @Override public String[] getSupportedCipherSuites() { return delegate.getSupportedCipherSuites(); }
        @Override public Socket createSocket() throws IOException { return delegate.createSocket(); }
        @Override public Socket createSocket(String h, int p) throws IOException { return delegate.createSocket(h, p); }
        @Override public Socket createSocket(String h, int p, InetAddress la, int lp) throws IOException { return delegate.createSocket(h, p, la, lp); }
        @Override public Socket createSocket(InetAddress h, int p) throws IOException { return delegate.createSocket(h, p); }
        @Override public Socket createSocket(InetAddress h, int p, InetAddress la, int lp) throws IOException { return delegate.createSocket(h, p, la, lp); }
    }

    private static final class DelegatingSSLContextSpi extends SSLContextSpi {
        private final SSLContext delegate;
        private final SSLSocketFactory factory;

        DelegatingSSLContextSpi(SSLContext delegate, SSLSocketFactory factory) {
            this.delegate = delegate;
            this.factory = factory;
        }

        @Override protected void engineInit(KeyManager[] km, TrustManager[] tm, SecureRandom sr) {}
        @Override protected SSLSocketFactory engineGetSocketFactory() { return factory; }
        @Override protected SSLServerSocketFactory engineGetServerSocketFactory() { return delegate.getServerSocketFactory(); }
        @Override protected SSLEngine engineCreateSSLEngine() { return delegate.createSSLEngine(); }
        @Override protected SSLEngine engineCreateSSLEngine(String host, int port) { return delegate.createSSLEngine(host, port); }
        @Override protected SSLSessionContext engineGetServerSessionContext() { return delegate.getServerSessionContext(); }
        @Override protected SSLSessionContext engineGetClientSessionContext() { return delegate.getClientSessionContext(); }
    }

    private void disconnect(FTPSClient client, long printerId) {
        try {
            if (client.isConnected()) {
                client.logout();
                client.disconnect();
            }
        } catch (FTPConnectionClosedException e) {
            // Server closed after completing the operation — normal for BambuLab FTPS
        } catch (IOException e) {
            logger.warn("Error disconnecting FTPS for printer [ID={}]", printerId, e);
        }
    }
}
