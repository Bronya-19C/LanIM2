package com.alpha.lanim.ui;

import com.alpha.lanim.client.ChatCoordinator;
import com.alpha.lanim.model.JoinAckPayload;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class ChatView {

    private ChatView() {
    }

    public static Scene create(Stage stage, JoinAckPayload ack, ChatCoordinator coordinator, String localNickname) {
        MemberPane members = coordinator.getMemberPane();
        ChatPane chat = coordinator.getChatPane();
        PreviewPane preview = coordinator.getPreviewPane();

        chat.setOnSend(() -> coordinator.sendText(chat.getInputTextAndClear()));
        chat.setOnFile(() -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select File to Send");
            File file = chooser.showOpenDialog(stage);
            if (file == null) {
                return;
            }
            new Thread(() -> {
                try {
                    Path path = file.toPath();
                    coordinator.sendFile(path);
                    Platform.runLater(() -> {
                        chat.appendFileNotice("You", file.getName(), file.length());
                        preview.showFile(path,
                                PreviewPane.resolveContentType(null, file.getName()),
                                file.getName());
                    });
                } catch (IOException ex) {
                    Platform.runLater(() ->
                            new Alert(Alert.AlertType.ERROR, "Failed to send file: " + ex.getMessage()).show());
                }
            }, "file-send").start();
        });

        BorderPane root = new BorderPane();
        root.getStyleClass().add("main-layout");
        root.setLeft(members.getNode());
        root.setCenter(chat.getNode());
        root.setRight(preview.getNode());

        String roomId = ack.getRoomId();
        String roomShort = roomId != null && !roomId.isEmpty()
                ? roomId.substring(0, Math.min(8, roomId.length())) + "..."
                : "?";
        stage.setTitle("LANIM - " + localNickname + " @ " + roomShort);
        stage.setMinWidth(1000);
        stage.setMinHeight(600);

        Scene scene = new Scene(root, 1050, 650);
        Styles.apply(scene);
        return scene;
    }
}
