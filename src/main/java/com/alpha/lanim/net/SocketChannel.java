package com.alpha.lanim.net;

import java.io.IOException;
import java.net.Socket;
import java.io.*;
import java.nio.ByteBuffer;
import javax.net.ssl.SSLSocket;

public class SocketChannel {

    private final Socket socket;
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private boolean open;

    private SocketChannel(Socket socket) throws IOException {
        this.socket = socket;
        this.dis = new DataInputStream(socket.getInputStream());
        this.dos = new DataOutputStream(socket.getOutputStream());
        this.open = true;
    }

    public static SocketChannel connect(String host, int port, boolean useTls, TlsContext tls) throws IOException {
        Socket sock;
        if (useTls) {
            javax.net.ssl.SSLContext ctx = tls.clientContext();
            sock = ctx.getSocketFactory().createSocket(host, port);
            ((SSLSocket) sock).startHandshake();   // 完成 TLS 握手
        } else {
            sock = new Socket(host, port);
        }
        return new SocketChannel(sock);
    }

    public static SocketChannel fromSocket(Socket socket, boolean useTls) throws IOException {
        return new SocketChannel(socket);
    }

    public void writeFrame(byte[] frame) throws IOException {
        dos.write(frame);
    }

    public byte[] readFrame() throws IOException {
        int length = dis.readInt();
        if (length <= 0 || length > 104857600){
            throw new IOException("Oversize Frame");
        }
        byte[] frame = new byte[4 + length];
        ByteBuffer.wrap(frame).putInt(length);
        dis.readFully(frame,4, length);
        return frame;
    }

    public void close() throws IOException {
        if (!open) {
            return;
        }
        open = false;
        try {
            socket.close();
        } catch (IOException e) {
            // Logger.log("Error closing socket", e);
            throw e;
        }
    }

    public boolean isOpen() {
        return this.open;
    }
}
