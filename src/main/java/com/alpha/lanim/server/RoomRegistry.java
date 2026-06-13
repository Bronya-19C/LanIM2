package com.alpha.lanim.server;

import com.alpha.lanim.data.MessageRepo;
import com.alpha.lanim.model.Envelope;
import com.alpha.lanim.model.JoinAckPayload;
import com.alpha.lanim.net.FrameCodec;
import com.alpha.lanim.util.Constants;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RoomRegistry {

    private final Map<String, Map<String, ClientSession>> roomSessions = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> roomMembers = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> roomSeq = new ConcurrentHashMap<>();
    private final MessageRepo messageRepo = new MessageRepo();

    public RoomRegistry() {
    }

    public void join(String roomId, String peerId, String nickname, ClientSession session) {
        roomSessions.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(peerId, session);
        roomMembers.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(peerId, nickname);
        roomSeq.computeIfAbsent(roomId, k -> {
            try {
                return new AtomicInteger(messageRepo.maxSequence(roomId));
            } catch (java.sql.SQLException e) {
                e.printStackTrace();
                return new AtomicInteger(0);
            }
        });
    }

    public void leave(String roomId, String peerId) {
        Map<String, ClientSession> sessions = roomSessions.get(roomId);
        if (sessions != null) {
            sessions.remove(peerId);
        }
        Map<String, String> members = roomMembers.get(roomId);
        if (members != null) {
            members.remove(peerId);
            if (members.isEmpty()) {
                roomMembers.remove(roomId);
                roomSessions.remove(roomId);
                roomSeq.remove(roomId);
            }
        }
    }

    public int nextSeq(String roomId) {
        AtomicInteger seq = roomSeq.get(roomId);
        if (seq == null) {
            return 0;
        }
        return seq.incrementAndGet();
    }

    public void saveAndTrim(Envelope env) {
        try {
            messageRepo.insert(env);
            messageRepo.trimRoom(env.getRoomId(), Constants.ROOM_MESSAGE_CAP);
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Envelope> recent(String roomId, int limit) {
        try {
            return messageRepo.recentByRoom(roomId, limit);
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            return List.of();
        }
    }

    public List<JoinAckPayload.MemberInfo> members(String roomId) {
        Map<String, String> memberMap = roomMembers.get(roomId);
        if (memberMap == null) {
            return List.of();
        }
        List<JoinAckPayload.MemberInfo> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : memberMap.entrySet()) {
            list.add(new JoinAckPayload.MemberInfo(entry.getKey(), entry.getValue()));
        }
        return list;
    }

    public void broadcastExcept(String roomId, ClientSession exclude, Envelope env) {
        byte[] framed = FrameCodec.encode(env);
        broadcastExcept(roomId, exclude, framed);
    }

    public void broadcastExcept(String roomId, ClientSession exclude, byte[] framed) {
        Map<String, ClientSession> sessions = roomSessions.get(roomId);
        if (sessions == null) {
            return;
        }
        for (ClientSession session : sessions.values()) {
            if (session != exclude) {
                session.sendRaw(framed);
            }
        }
    }

    public void shutdown() {
        for (Map<String, ClientSession> sessions : roomSessions.values()) {
            for (ClientSession session : sessions.values()) {
                session.sendRaw(null);
            }
        }
    }
}
