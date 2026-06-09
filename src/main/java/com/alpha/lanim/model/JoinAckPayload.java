package com.alpha.lanim.model;

import java.util.List;

public class JoinAckPayload {

    private String assignedId;
    private String roomId;
    private List<Envelope> history;
    private List<MemberInfo> members;

    public JoinAckPayload() {
    }

    public JoinAckPayload(String assignedId, String roomId, List<Envelope> history, List<MemberInfo> members) {
    }

    public String getAssignedId() {
        return null;
    }

    public void setAssignedId(String assignedId) {
    }

    public String getRoomId() {
        return null;
    }

    public void setRoomId(String roomId) {
    }

    public List<Envelope> getHistory() {
        return null;
    }

    public void setHistory(List<Envelope> history) {
    }

    public List<MemberInfo> getMembers() {
        return null;
    }

    public void setMembers(List<MemberInfo> members) {
    }

    public static class MemberInfo {

        private String peerId;
        private String nickname;

        public MemberInfo() {
        }

        public MemberInfo(String peerId, String nickname) {
        }

        public String getPeerId() {
            return null;
        }

        public void setPeerId(String peerId) {
        }

        public String getNickname() {
            return null;
        }

        public void setNickname(String nickname) {
        }
    }
}
