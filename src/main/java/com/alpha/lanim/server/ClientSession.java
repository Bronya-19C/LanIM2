package com.alpha.lanim.server;

import com.alpha.lanim.model.*;
import com.alpha.lanim.net.FrameCodec;
import com.alpha.lanim.net.SocketChannel;
import com.alpha.lanim.util.Constants;
import com.alpha.lanim.util.HashUtil;
import com.alpha.lanim.util.JsonUtil;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class ClientSession implements Runnable {

    private final SocketChannel channel;
    private final RoomRegistry registry;
    private final Object sendLock = new Object();

    private String peerId;
    private String nickname;
    private String roomId;
    private boolean joined;

    public ClientSession(SocketChannel channel, RoomRegistry registry) {
        this.channel = channel;
        this.registry = registry;
    }

    @Override
    public void run() {
        try {
            while (channel.isOpen()) {
                byte[] frame = channel.readFrame();
                if (frame == null) {
                    break;
                }
                Envelope env = FrameCodec.decode(frame);
                if (env == null) {
                    continue;
                }

                String type = env.getType();
                if (MessageType.JOIN.name().equals(type)) {
                    onJoin(env);
                } else if (MessageType.CHAT_TEXT.name().equals(type)) {
                    onChat(env);
                } else if (type != null && type.startsWith("FILE_")) {
                    onFile(env);
                }
            }
        } catch (IOException e) {
            // connection closed by peer
        } finally {
            onDisconnect();
        }
    }

    void send(Envelope env) {
        byte[] frame = FrameCodec.encode(env);
        sendRaw(frame);
    }

    void sendRaw(byte[] framed) {
        if (framed == null) {
            return;
        }
        synchronized (sendLock) {
            try {
                channel.writeFrame(framed);
            } catch (IOException e) {
                // session will be cleaned up by read loop detecting the error
            }
        }
    }

    private void onJoin(Envelope env) {
        JoinPayload jp = JsonUtil.fromPayload(env.getPayload(), JoinPayload.class);
        if (jp == null) {
            return;
        }
        peerId = jp.getPeerId();
        nickname = jp.getNickname();
        roomId = HashUtil.sha512Hex(jp.getRoomSecret());

        joined = true;
        registry.join(roomId, peerId, nickname, this);

        List<Envelope> history = registry.recent(roomId, Constants.JOIN_HISTORY_LIMIT);
        List<JoinAckPayload.MemberInfo> members = registry.members(roomId);
        JoinAckPayload ackPayload = new JoinAckPayload(peerId, roomId, history, members);
        Envelope ack = new Envelope(
                MessageType.JOIN_ACK.name(),
                UUID.randomUUID().toString(),
                "SERVER",
                roomId,
                0,
                System.currentTimeMillis(),
                ackPayload
        );
        send(ack);

        UserEventPayload uep = new UserEventPayload(peerId, nickname);
        Envelope joinedEvent = new Envelope(
                MessageType.USER_JOINED.name(),
                UUID.randomUUID().toString(),
                peerId,
                roomId,
                0,
                System.currentTimeMillis(),
                uep
        );
        registry.broadcastExcept(roomId, this, joinedEvent);
    }

    private void onChat(Envelope env) {
        if (!joined) {
            return;
        }
        env.setRoomId(roomId);
        env.setSequence(registry.nextSeq(roomId));
        env.setTimestamp(System.currentTimeMillis());
        registry.saveAndTrim(env);
        registry.broadcastExcept(roomId, this, env);
    }

    private void onFile(Envelope env) {
        if (!joined) {
            return;
        }
        env.setRoomId(roomId);
        String type = env.getType();
        if (MessageType.FILE_META.name().equals(type)) {
            env.setSequence(registry.nextSeq(roomId));
            env.setTimestamp(System.currentTimeMillis());
            registry.saveAndTrim(env);
        }
        registry.broadcastExcept(roomId, this, env);
    }

    private void onDisconnect() {
        if (joined) {
            joined = false;
            registry.leave(roomId, peerId);
            UserEventPayload uep = new UserEventPayload(peerId, nickname);
            Envelope leftEvent = new Envelope(
                    MessageType.USER_LEFT.name(),
                    UUID.randomUUID().toString(),
                    peerId,
                    roomId,
                    0,
                    System.currentTimeMillis(),
                    uep
            );
            registry.broadcastExcept(roomId, this, leftEvent);
        }
        channel.close();
    }
}
