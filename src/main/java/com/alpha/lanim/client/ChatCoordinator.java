package com.alpha.lanim.client;

import com.alpha.lanim.data.MessageRepo;
import com.alpha.lanim.model.*;
import com.alpha.lanim.ui.ChatPane;
import com.alpha.lanim.ui.MemberPane;
import com.alpha.lanim.ui.PreviewPane;
import com.alpha.lanim.util.Constants;
import com.alpha.lanim.util.JsonUtil;
import com.alpha.lanim.util.Validator;
import javafx.application.Platform;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatCoordinator implements ImClient.InboundListener {

    private final ImClient client;
    private final MessageRepo repo;
    private final FileTransfer fileTransfer;
    private final MemberPane members;
    private final ChatPane chat;
    private final PreviewPane preview;
    private final String localPeerId;
    private final String localNickname;
    private final String roomId;

    private final Map<String, String> peerNicknames = new ConcurrentHashMap<>();

    public ChatCoordinator(ImClient client, MessageRepo repo, FileTransfer fileTransfer,
                           MemberPane members, ChatPane chat, PreviewPane preview,
                           String localPeerId, String localNickname, String roomId) {
        this.client = client;
        this.repo = repo;
        this.fileTransfer = fileTransfer;
        this.members = members;
        this.chat = chat;
        this.preview = preview;
        this.localPeerId = localPeerId;
        this.localNickname = localNickname;
        this.roomId = roomId;

        client.setListener(this);
    }

    public MemberPane getMemberPane() {
        return members;
    }

    public ChatPane getChatPane() {
        return chat;
    }

    public PreviewPane getPreviewPane() {
        return preview;
    }

    public void onJoined(JoinAckPayload ack) {
        if (ack.getMembers() != null) {
            for (JoinAckPayload.MemberInfo mi : ack.getMembers()) {
                peerNicknames.put(mi.getPeerId(), mi.getNickname());
                fileTransfer.onUserJoined(mi.getPeerId(), mi.getNickname());
            }
        }

        Platform.runLater(() -> {
            if (ack.getMembers() != null) {
                // MemberPane.setMembers expects List<MemberRow>, but we have List<MemberInfo>
                // We convert MemberInfo to MemberRow
                java.util.List<MemberRow> rows = new java.util.ArrayList<>();
                for (JoinAckPayload.MemberInfo mi : ack.getMembers()) {
                    rows.add(new MemberRow(mi.getPeerId(), mi.getNickname()));
                }
                members.setMembers(localNickname, rows);
            }
            if (ack.getHistory() != null) {
                chat.loadHistory(ack.getHistory());
            }
        });

        if (ack.getHistory() != null) {
            for (Envelope env : ack.getHistory()) {
                try {
                    repo.insert(env);
                } catch (java.sql.SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onEnvelope(Envelope env) {
        String type = env.getType();
        if (type == null) {
            return;
        }

        switch (type) {
            case "USER_JOINED": {
                UserEventPayload uep = JsonUtil.fromPayload(env.getPayload(), UserEventPayload.class);
                if (uep == null) break;
                peerNicknames.put(uep.getPeerId(), uep.getNickname());
                fileTransfer.onUserJoined(uep.getPeerId(), uep.getNickname());
                Platform.runLater(() -> members.addMember(uep.getPeerId(), uep.getNickname()));
                break;
            }
            case "USER_LEFT": {
                UserEventPayload uep = JsonUtil.fromPayload(env.getPayload(), UserEventPayload.class);
                if (uep == null) break;
                peerNicknames.remove(uep.getPeerId());
                fileTransfer.onUserLeft(uep.getPeerId());
                Platform.runLater(() -> members.removeMember(uep.getPeerId()));
                break;
            }
            case "CHAT_TEXT": {
                try {
                    repo.insert(env);
                } catch (java.sql.SQLException e) {
                    e.printStackTrace();
                }
                if (!localPeerId.equals(env.getSenderId())) {
                    String senderLabel = peerNicknames.getOrDefault(env.getSenderId(), env.getSenderId());
                    Platform.runLater(() -> chat.append(env, senderLabel));
                }
                break;
            }
            case "FILE_META":
            case "FILE_CHUNK":
            case "FILE_CHUNK_ACK": {
                fileTransfer.handle(env);
                break;
            }
            case "JOIN_ACK":
                // already handled by ImClient.connectAndJoin; ignore duplicates
                break;
        }
    }

    public void sendText(String text) {
        String error = Validator.validateChatText(text);
        if (error != null) {
            return;
        }
        ChatPayload cp = new ChatPayload(text);
        Envelope env = new Envelope(
                MessageType.CHAT_TEXT.name(),
                UUID.randomUUID().toString(),
                localPeerId,
                roomId,
                0,
                System.currentTimeMillis(),
                cp
        );
        try {
            client.send(env);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Platform.runLater(() -> chat.appendLocalText(text, localNickname));
    }

    public void sendFile(Path path) throws IOException {
        fileTransfer.send(path, localPeerId, roomId);
    }
}
