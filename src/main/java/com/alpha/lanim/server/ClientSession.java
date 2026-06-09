package com.alpha.lanim.server;

import com.alpha.lanim.net.SocketChannel;
import com.alpha.lanim.model.Envelope;

public class ClientSession implements Runnable {

    public ClientSession(SocketChannel channel, RoomRegistry registry) {
    }

    @Override
    public void run() {
    }

    void send(Envelope env) {
    }

    void sendRaw(byte[] framed) {
    }

    private void onJoin(Envelope env) {
    }

    private void onChat(Envelope env) {
    }

    private void onFile(Envelope env) {
    }

    private void onDisconnect() {
    }
}
