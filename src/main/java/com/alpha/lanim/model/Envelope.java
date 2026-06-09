package com.alpha.lanim.model;

public class Envelope {

    private String type;
    private String messageId;
    private String senderId;
    private String roomId;
    private int sequence;
    private long timestamp;
    private Object payload;

    public Envelope() {
    }

    public Envelope(String type, String messageId, String senderId, String roomId,
                    int sequence, long timestamp, Object payload) {
    }

    public String getType() {
        return null;
    }

    public void setType(String type) {
    }

    public String getMessageId() {
        return null;
    }

    public void setMessageId(String messageId) {
    }

    public String getSenderId() {
        return null;
    }

    public void setSenderId(String senderId) {
    }

    public String getRoomId() {
        return null;
    }

    public void setRoomId(String roomId) {
    }

    public int getSequence() {
        return 0;
    }

    public void setSequence(int sequence) {
    }

    public long getTimestamp() {
        return 0L;
    }

    public void setTimestamp(long timestamp) {
    }

    public Object getPayload() {
        return null;
    }

    public void setPayload(Object payload) {
    }
}
