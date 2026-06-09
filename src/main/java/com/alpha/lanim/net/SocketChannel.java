package com.alpha.lanim.net;

import java.io.IOException;
import java.net.Socket;

public class SocketChannel {

    SocketChannel() {
    }

    public static SocketChannel connect(String host, int port, boolean useTls, TlsContext tls) throws IOException {
        return null;
    }

    public static SocketChannel fromSocket(Socket socket, boolean useTls) {
        return null;
    }

    public void writeFrame(byte[] frame) throws IOException {
    }

    public byte[] readFrame() throws IOException {
        return null;
    }

    public void close() {
    }

    public boolean isOpen() {
        return false;
    }
}
