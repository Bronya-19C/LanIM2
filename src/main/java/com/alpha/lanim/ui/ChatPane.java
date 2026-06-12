package com.alpha.lanim.ui;

import com.alpha.lanim.model.ChatPayload;
import com.alpha.lanim.model.Envelope;
import com.alpha.lanim.model.FileMetaPayload;
import com.alpha.lanim.util.JsonUtil;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatPane {

    private final VBox root;
    private final VBox messageContainer;
    private final ScrollPane scrollPane;
    private final TextField messageField;
    private final Set<String> displayedMessageIds = new HashSet<>();
    private Runnable onSendAction;
    private Runnable onFileAction;

    public ChatPane() {
        messageContainer = new VBox(6);
        messageContainer.setPadding(new Insets(10));

        scrollPane = new ScrollPane(messageContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        messageField = new TextField();
        messageField.setPromptText("Type a message...");

        Button sendButton = new Button("Send");
        sendButton.setPrefWidth(80);

        Button fileButton = new Button("File");
        fileButton.setPrefWidth(60);
        fileButton.setTooltip(new Tooltip("Send a file"));

        HBox inputBox = new HBox(10);
        inputBox.setPadding(new Insets(10));
        inputBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(messageField, Priority.ALWAYS);
        inputBox.getChildren().addAll(messageField, sendButton, fileButton);

        Runnable sendMessage = () -> {
            if (onSendAction != null) {
                onSendAction.run();
            }
        };
        sendButton.setOnAction(e -> sendMessage.run());
        messageField.setOnAction(e -> sendMessage.run());
        fileButton.setOnAction(e -> {
            if (onFileAction != null) {
                onFileAction.run();
            }
        });

        root = new VBox();
        root.getChildren().addAll(scrollPane, inputBox);
    }

    public Node getNode() {
        return root;
    }

    public String getInputTextAndClear() {
        String text = messageField.getText().trim();
        messageField.clear();
        return text;
    }

    public void loadHistory(List<Envelope> messages) {
        messageContainer.getChildren().clear();
        displayedMessageIds.clear();
        if (messages == null || messages.isEmpty()) {
            return;
        }
        messages.stream()
                .sorted(Comparator.comparingInt(Envelope::getSequence))
                .forEach(this::renderEnvelope);
        scrollToBottom();
    }

    public void append(Envelope env, String senderLabel) {
        if (env == null) {
            return;
        }
        renderEnvelope(env, senderLabel);
        scrollToBottom();
    }

    public void appendLocalText(String text, String nickname) {
        Label label = new Label(nickname + " (You): " + text);
        label.setWrapText(true);
        label.setStyle("-fx-font-size: 13px; -fx-padding: 3 0; -fx-text-fill: #1a1a1a;");
        messageContainer.getChildren().add(label);
        scrollToBottom();
    }

    public void appendFileNotice(String senderLabel, String fileName, long sizeBytes) {
        String who = "You".equals(senderLabel) ? "You" : senderLabel;
        Label label = new Label(who + " sent file: " + fileName + " (" + (sizeBytes / 1024) + " KB)");
        label.setWrapText(true);
        if ("You".equals(senderLabel)) {
            label.setStyle("-fx-text-fill: green; -fx-font-style: italic;");
        } else {
            label.setStyle("-fx-text-fill: #0066cc; -fx-font-style: italic;");
        }
        messageContainer.getChildren().add(label);
        scrollToBottom();
    }

    public void setOnSend(Runnable action) {
        this.onSendAction = action;
    }

    public void setOnFile(Runnable action) {
        this.onFileAction = action;
    }

    private void renderEnvelope(Envelope envelope) {
        renderEnvelope(envelope, shortSenderId(envelope.getSenderId()));
    }

    private void renderEnvelope(Envelope envelope, String senderLabel) {
        if (envelope.getMessageId() != null && !displayedMessageIds.add(envelope.getMessageId())) {
            return;
        }
        String type = envelope.getType();
        if (type == null) {
            return;
        }
        switch (type) {
            case "CHAT_TEXT" -> {
                ChatPayload payload = JsonUtil.fromPayload(envelope.getPayload(), ChatPayload.class);
                if (payload == null) {
                    return;
                }
                Label label = new Label(senderLabel + ": " + payload.getText());
                label.setWrapText(true);
                label.setStyle("-fx-font-size: 13px; -fx-padding: 3 0; -fx-text-fill: #333;");
                messageContainer.getChildren().add(label);
            }
            case "FILE_META" -> {
                FileMetaPayload meta = JsonUtil.fromPayload(envelope.getPayload(), FileMetaPayload.class);
                if (meta == null) {
                    return;
                }
                Label label = new Label(senderLabel + " sent file: " + meta.getFileName()
                        + " (" + (meta.getTotalSize() / 1024) + " KB)");
                label.setWrapText(true);
                label.setStyle("-fx-text-fill: #0066cc; -fx-font-style: italic;");
                messageContainer.getChildren().add(label);
            }
            default -> {
            }
        }
    }

    private static String shortSenderId(String senderId) {
        if (senderId == null) {
            return "Unknown";
        }
        return senderId.length() <= 8 ? senderId : senderId.substring(0, 8);
    }

    private void scrollToBottom() {
        scrollPane.layout();
        scrollPane.setVvalue(1.0);
    }
}
