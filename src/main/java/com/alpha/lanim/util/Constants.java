package com.alpha.lanim.util;

public final class Constants {

    public static final String DEFAULT_SERVER_HOST = "10.129.245.252";
    public static final int DEFAULT_SERVER_PORT = 9090;
    public static final int JOIN_HISTORY_LIMIT = 10;
    public static final int ROOM_MESSAGE_CAP = 100;
    public static final int FILE_CHUNK_SIZE = 65536;
    public static final String DEFAULT_DB_PATH = "data/lanim.db";
    public static final String DEFAULT_FILES_PATH = "data/files";
    public static final String DEFAULT_KEYSTORE_PATH = "config/keystore.jks";
    public static final String TRANSPORT_MODE_TLS = "tls";
    public static final String TRANSPORT_MODE_PLAIN = "plain";
    public static final String DEFAULT_TRANSPORT_MODE = "tls";
    public static final int MAX_NICKNAME_LENGTH = 32;
    public static final int MAX_TEXT_LENGTH = 4096;
    public static final int JOIN_TIMEOUT_MS = 15000;

    private Constants() {
    }
}
