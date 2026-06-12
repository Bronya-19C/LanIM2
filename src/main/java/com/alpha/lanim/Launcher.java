package com.alpha.lanim;

import com.alpha.lanim.client.ChatCoordinator;
import com.alpha.lanim.client.FileTransfer;
import com.alpha.lanim.client.ImClient;
import com.alpha.lanim.data.Database;
import com.alpha.lanim.data.FileRepo;
import com.alpha.lanim.data.MessageRepo;
import com.alpha.lanim.model.JoinAckPayload;
import com.alpha.lanim.net.TlsContext;
import com.alpha.lanim.ui.ChatPane;
import com.alpha.lanim.ui.ChatView;
import com.alpha.lanim.ui.LoginView;
import com.alpha.lanim.ui.MemberPane;
import com.alpha.lanim.ui.PreviewPane;
import com.alpha.lanim.util.Constants;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

import java.util.UUID;

public class Launcher extends Application {

    private ImClient client;
    private Stage stage;
    private TlsContext tlsContext;
    private final String localPeerId = UUID.randomUUID().toString();

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        Database.init();

        tlsContext = new TlsContext();
        try {
            tlsContext.init();
        } catch (Exception e) {
            e.printStackTrace();
        }

        stage.setOnCloseRequest(e -> shutdown());
        stage.setScene(LoginView.create(stage, this::doConnect));
        stage.show();
    }

    private void doConnect(String nickname, String roomSecret, boolean useTls, String serverAddress) {
        new Thread(() -> {
            try {
                client = new ImClient(tlsContext);
                HostPort hp = parseServerAddress(serverAddress);
                JoinAckPayload ack = client.connectAndJoin(
                        hp.host(), hp.port(), useTls, localPeerId, nickname, roomSecret);
                Platform.runLater(() -> showChatView(ack, nickname));
            } catch (Exception ex) {
                ex.printStackTrace();
                Platform.runLater(() -> {
                    new Alert(Alert.AlertType.ERROR,
                            "Connection failed: " + ex.getMessage()).showAndWait();
                    stage.setScene(LoginView.create(stage, this::doConnect));
                });
            }
        }, "connect").start();
    }

    private void showChatView(JoinAckPayload ack, String nickname) {
        MemberPane members = new MemberPane();
        ChatPane chat = new ChatPane();
        PreviewPane preview = new PreviewPane();

        MessageRepo messageRepo = new MessageRepo();
        FileRepo fileRepo = new FileRepo();
        FileTransfer fileTransfer = new FileTransfer(
                client, fileRepo, chat, preview, localPeerId, ack.getRoomId());
        ChatCoordinator coordinator = new ChatCoordinator(
                client, messageRepo, fileTransfer, members, chat, preview,
                localPeerId, nickname, ack.getRoomId());

        coordinator.onJoined(ack);
        stage.setScene(ChatView.create(stage, ack, coordinator, nickname));
    }

    private void shutdown() {
        if (client != null) {
            client.close();
        }
    }

    private static HostPort parseServerAddress(String serverAddress) {
        String host = Constants.DEFAULT_SERVER_HOST;
        int port = Constants.DEFAULT_SERVER_PORT;
        if (serverAddress != null && !serverAddress.isBlank()) {
            String s = serverAddress.trim();
            int colon = s.lastIndexOf(':');
            if (colon > 0 && colon < s.length() - 1) {
                host = s.substring(0, colon).trim();
                try {
                    port = Integer.parseInt(s.substring(colon + 1).trim());
                } catch (NumberFormatException ignored) {
                    port = Constants.DEFAULT_SERVER_PORT;
                }
            } else {
                host = s;
            }
        }
        return new HostPort(host, port);
    }

    private record HostPort(String host, int port) {
    }

    public static void main(String[] args) {
        launch(args);
    }
}
