package com.alpha.lanim.server;

import com.alpha.lanim.model.Envelope;
import com.alpha.lanim.model.JoinAckPayload;

import java.util.List;

public class RoomRegistry {

    public RoomRegistry() {
    }

    public void join(String roomId, String peerId, String nickname, ClientSession session) {
    }

    public void leave(String roomId, String peerId) {
    }

    public int nextSeq(String roomId) {
        return 0;
    }

    public void saveAndTrim(Envelope env) {
    }

    public List<Envelope> recent(String roomId, int limit) {
        return null;
    }

    public List<JoinAckPayload.MemberInfo> members(String roomId) {
        return null;
    }

    public void broadcastExcept(String roomId, ClientSession exclude, Envelope env) {
    }

    public void broadcastExcept(String roomId, ClientSession exclude, byte[] framed) {
    }

    public void shutdown() {
    }
}
