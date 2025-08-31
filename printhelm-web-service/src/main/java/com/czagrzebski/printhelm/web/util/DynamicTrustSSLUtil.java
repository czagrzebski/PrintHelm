package com.czagrzebski.printhelm.web.util;

import javax.net.SocketFactory;
import javax.net.ssl.*;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class DynamicTrustSSLUtil {

    /**
     * Fetch the SSL certificate from the MQTT broker (host:port).
     */
    public static X509Certificate fetchBrokerCertificateTrustAll(String host, int port) throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        SSLSocketFactory factory = sslContext.getSocketFactory();

        try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
            socket.startHandshake();
            SSLSession session = socket.getSession();
            Certificate[] certs = session.getPeerCertificates();
            return (X509Certificate) certs[0];
        }
    }

    /**
     * Create SSLContext trusting only the given certificate.
     */
    public static SSLContext createSslContextWithCert(X509Certificate cert) throws Exception {
        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(null, null);
        trustStore.setCertificateEntry("broker", cert);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, tmf.getTrustManagers(), new SecureRandom());

        return sslContext;
    }

    /**
     * Wrap the SSLContext's SocketFactory disabling hostname verification.
     */
    public static SSLSocketFactory createSocketFactoryDisablingHostnameVerification(SSLContext sslContext) {
        final SSLSocketFactory baseFactory = sslContext.getSocketFactory();

        return new SSLSocketFactory() {
            @Override
            public String[] getDefaultCipherSuites() {
                return baseFactory.getDefaultCipherSuites();
            }

            @Override
            public String[] getSupportedCipherSuites() {
                return baseFactory.getSupportedCipherSuites();
            }

            private Socket wrap(Socket socket) {
                if (socket instanceof SSLSocket sslSocket) {
                    SSLParameters params = sslSocket.getSSLParameters();
                    params.setEndpointIdentificationAlgorithm(null); // disable hostname verification
                    sslSocket.setSSLParameters(params);
                }
                return socket;
            }

            @Override
            public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws java.io.IOException {
                return wrap(baseFactory.createSocket(s, host, port, autoClose));
            }

            @Override
            public Socket createSocket(String host, int port) throws java.io.IOException {
                return wrap(baseFactory.createSocket(host, port));
            }

            @Override
            public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws java.io.IOException {
                return wrap(baseFactory.createSocket(host, port, localHost, localPort));
            }

            @Override
            public Socket createSocket(InetAddress host, int port) throws java.io.IOException {
                return wrap(baseFactory.createSocket(host, port));
            }

            @Override
            public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws java.io.IOException {
                return wrap(baseFactory.createSocket(address, port, localAddress, localPort));
            }

            @Override
            public Socket createSocket() throws java.io.IOException {
                return wrap(baseFactory.createSocket());
            }
        };
    }

    /**
     * Convenience method: Create a SocketFactory that trusts the broker's cert dynamically and disables hostname verification.
     */
    public static SocketFactory createDynamicTrustSocketFactory(String host, int port) throws Exception {
        X509Certificate cert = fetchBrokerCertificateTrustAll(host, port);
        SSLContext sslContext = createSslContextWithCert(cert);
        return createSocketFactoryDisablingHostnameVerification(sslContext);
    }
}