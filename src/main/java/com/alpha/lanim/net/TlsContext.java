package com.alpha.lanim.net;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;

public class TlsContext {

    private static final String DEFAULT_KEYSTORE_PATH = "config/keystore.jks";
    private static final String DEFAULT_KEYSTORE_PASSWORD = "lanim123";

    private SSLContext sslContext;
    private final String keyStorePath;
    private final String keyStorePassword;
    private final String trustStorePath;
    private final String trustStorePassword;

    public TlsContext() {
        this(
                System.getProperty("javax.net.ssl.keyStore"),
                System.getProperty("javax.net.ssl.keyStorePassword"),
                System.getProperty("javax.net.ssl.trustStore"),
                System.getProperty("javax.net.ssl.trustStorePassword")
        );
    }

    public TlsContext(String keyStorePath, String keyStorePassword,
                      String trustStorePath, String trustStorePassword) {
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
        this.trustStorePath = trustStorePath;
        this.trustStorePassword = trustStorePassword;
    }

    public void init() throws Exception {
        KeyManager[] keyManagers = loadKeyManagers();

        TrustManager[] trustManagers = null;
        if (trustStorePath != null && !trustStorePath.isEmpty()) {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream tsIs = new FileInputStream(trustStorePath)) {
                trustStore.load(tsIs, trustStorePassword.toCharArray());
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            trustManagers = tmf.getTrustManagers();
        } else {
            trustManagers = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    }
            };
            System.err.println("WARNING: Using insecure TrustManager (trust all certificates)");
        }

        sslContext = SSLContext.getInstance("TLS");
        sslContext.init(keyManagers, trustManagers, new SecureRandom());
    }

    private KeyManager[] loadKeyManagers() throws Exception {
        String path = keyStorePath;
        String password = keyStorePassword;

        if (path == null || path.isEmpty()) {
            path = DEFAULT_KEYSTORE_PATH;
            password = DEFAULT_KEYSTORE_PASSWORD;
        }

        Path p = Paths.get(path);
        if (Files.notExists(p)) {
            Path parent = p.getParent();
            if (parent != null && Files.notExists(parent)) {
                Files.createDirectories(parent);
            }
            generateKeystore(p, password != null ? password : DEFAULT_KEYSTORE_PASSWORD);
            System.err.println("Generated new keystore: " + path);
        }

        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        try (InputStream ksIs = new FileInputStream(path)) {
            keyStore.load(ksIs, (password != null ? password : "").toCharArray());
        }
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, (password != null ? password : "").toCharArray());
        return kmf.getKeyManagers();
    }

    private void generateKeystore(Path path, String password) throws Exception {
        String[] cmd = {
                "keytool", "-genkeypair",
                "-alias", "lanim",
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-sigalg", "SHA256withRSA",
                "-dname", "CN=LanIM Server",
                "-validity", "365",
                "-keystore", path.toAbsolutePath().toString(),
                "-storepass", password,
                "-keypass", password
        };
        Process proc = Runtime.getRuntime().exec(cmd);
        proc.getOutputStream().close();
        int exit = proc.waitFor();
        if (exit != 0) {
            byte[] err = proc.getErrorStream().readAllBytes();
            throw new RuntimeException("keytool failed: " + new String(err));
        }
    }

    public SSLContext serverContext() {
        return sslContext;
    }

    public SSLContext clientContext() {
        return sslContext;
    }
}
