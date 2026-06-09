package com.alpha.lanim.ui;

import com.alpha.lanim.model.Envelope;
import javafx.scene.Node;

import java.util.List;

public class ChatPane {

    public ChatPane() {
    }

    public Node getNode() {
        return null;
    }

    public void loadHistory(List<Envelope> messages) {
    }

    public void append(Envelope env, String senderLabel) {
    }

    public void appendLocalText(String text, String nickname) {
    }

    public void appendFileNotice(String senderLabel, String fileName, long sizeBytes) {
    }

    public void setOnSend(Runnable action) {
    }

    public void setOnFile(Runnable action) {
    }
}
