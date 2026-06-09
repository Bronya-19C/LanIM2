package com.alpha.lanim.client;

import com.alpha.lanim.ui.MemberPane;
import com.alpha.lanim.ui.ChatPane;
import com.alpha.lanim.ui.PreviewPane;
import com.alpha.lanim.data.MessageRepo;
import com.alpha.lanim.model.Envelope;
import com.alpha.lanim.model.JoinAckPayload;

import java.io.IOException;
import java.nio.file.Path;

public class ChatCoordinator implements ImClient.InboundListener {

    public ChatCoordinator(ImClient client, MessageRepo repo, FileTransfer fileTransfer,
                           MemberPane members, ChatPane chat, PreviewPane preview,
                           String localPeerId, String localNickname, String roomId) {
    }

    public void onJoined(JoinAckPayload ack) {
    }

    @Override
    public void onEnvelope(Envelope env) {
    }

    public void sendText(String text) {
    }

    public void sendFile(Path path) throws IOException {
    }
}
