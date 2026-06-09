package com.alpha.lanim.ui;

import javafx.scene.Scene;
import javafx.stage.Stage;

public class LoginView {

    public interface ConnectCallback {
        void onConnect(String nickname, String roomSecret, boolean useTls, String serverAddress);
    }

    private LoginView() {
    }

    public static Scene create(Stage stage, ConnectCallback callback) {
        return null;
    }
}
