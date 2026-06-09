package com.alpha.lanim;

import com.alpha.lanim.client.ImClient;
import com.alpha.lanim.model.JoinAckPayload;
import javafx.application.Application;
import javafx.stage.Stage;

public class Launcher extends Application {

    @Override
    public void start(Stage stage) {
    }

    private void doConnect(String nickname, String roomSecret, boolean useTls, String serverAddress) {
    }

    private void showChatView(JoinAckPayload ack, ImClient client) {
    }

    private void shutdown() {
    }

    public static void main(String[] args) {
        launch(args);
    }
}
