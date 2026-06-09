package com.alpha.lanim.data;

import com.alpha.lanim.model.Envelope;

import java.util.List;

public class MessageRepo {

    public MessageRepo() {
    }

    public void insert(Envelope e) {
    }

    public List<Envelope> recentByRoom(String roomId, int limit) {
        return null;
    }

    public void trimRoom(String roomId, int maxCount) {
    }

    public int maxSequence(String roomId) {
        return 0;
    }
}
