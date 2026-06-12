package com.alpha.lanim.net;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

public class TlsContext {

    private SSLContext sslContext;
    private final String keyStorePath;
    private final String keyStorePassword;
    private final String trustStorePath;
    private final String trustStorePassword;

    /**
     * 默认构造器，尝试从系统属性或默认路径加载证书。
     * 系统属性:
     * - javax.net.ssl.keyStore
     * - javax.net.ssl.keyStorePassword
     * - javax.net.ssl.trustStore
     * - javax.net.ssl.trustStorePassword
     */
    public TlsContext() {
        this(
                System.getProperty("javax.net.ssl.keyStore"),
                System.getProperty("javax.net.ssl.keyStorePassword"),
                System.getProperty("javax.net.ssl.trustStore"),
                System.getProperty("javax.net.ssl.trustStorePassword")
        );
    }

    /**
     * 使用显式路径和密码构建。
     * @param keyStorePath     密钥库路径（可为null，表示不使用客户端认证）
     * @param keyStorePassword 密钥库密码
     * @param trustStorePath   信任库路径（可为null，使用默认信任链）
     * @param trustStorePassword 信任库密码
     */
    public TlsContext(String keyStorePath, String keyStorePassword,
                      String trustStorePath, String trustStorePassword) {
        this.keyStorePath = keyStorePath;
        this.keyStorePassword = keyStorePassword;
        this.trustStorePath = trustStorePath;
        this.trustStorePassword = trustStorePassword;
    }

    /**
     * 初始化 SSLContext，需在使用前调用。
     */
    public void init() throws Exception {
        KeyManager[] keyManagers = null;
        if (keyStorePath != null && !keyStorePath.isEmpty()) {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream ksIs = new FileInputStream(keyStorePath)) {
                keyStore.load(ksIs, keyStorePassword.toCharArray());
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, keyStorePassword.toCharArray());
            keyManagers = kmf.getKeyManagers();
        }

        TrustManager[] trustManagers = null;
        if (trustStorePath != null && !trustStorePath.isEmpty()) {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try (InputStream tsIs = new FileInputStream(trustStorePath)) {
                trustStore.load(tsIs, trustStorePassword.toCharArray());
            }
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(trustStore);
            trustManagers = tmf.getTrustManagers();
        } else if (keyStorePath == null && trustStorePath == null) {
            // 没有任何证书时，使用一个信任所有证书的 TrustManager（仅用于开发测试！）
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

    /**
     * 返回服务端使用的 SSLContext（与客户端共用同一个，除非你希望分开）。
     */
    public SSLContext serverContext() {
        return sslContext;
    }

    /**
     * 返回客户端使用的 SSLContext（与服务器共用）。
     */
    public SSLContext clientContext() {
        return sslContext;
    }
}