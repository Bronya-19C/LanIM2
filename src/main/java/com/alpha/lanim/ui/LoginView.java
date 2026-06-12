package com.alpha.lanim.ui;

import com.alpha.lanim.util.Constants;
import com.alpha.lanim.util.Validator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginView {

    public interface ConnectCallback {
        void onConnect(String nickname, String roomSecret, boolean useTls, String serverAddress);
    }

    private LoginView() {
    }

    public static Scene create(Stage stage, ConnectCallback callback) {
        Label titleLabel = new Label("LANIM");
        titleLabel.getStyleClass().add("login-title");

        Label subtitleLabel = new Label("Local Area Network Instant Messenger");
        subtitleLabel.getStyleClass().add("login-subtitle");

        Separator sep = new Separator();
        sep.getStyleClass().add("login-separator");
        sep.setMaxWidth(300);

        Label serverLabel = new Label("Server Address");
        serverLabel.getStyleClass().add("login-label");
        TextField serverField = new TextField();
        serverField.getStyleClass().add("login-field");
        serverField.setPromptText(Constants.DEFAULT_SERVER_HOST + ":" + Constants.DEFAULT_SERVER_PORT);
        serverField.setText(Constants.DEFAULT_SERVER_HOST + ":" + Constants.DEFAULT_SERVER_PORT);

        Label nicknameLabel = new Label("Nickname");
        nicknameLabel.getStyleClass().add("login-label");
        TextField nicknameField = new TextField();
        nicknameField.getStyleClass().add("login-field");
        nicknameField.setPromptText("Enter your display name...");
        nicknameField.setText("User-" + Integer.toHexString((int) (Math.random() * 0xFFFF)).toUpperCase());

        Label roomLabel = new Label("Room Secret");
        roomLabel.getStyleClass().add("login-label");
        PasswordField roomField = new PasswordField();
        roomField.getStyleClass().add("login-field");
        roomField.setPromptText("Enter room password...");

        CheckBox tlsCheckbox = new CheckBox("Enable TLS encryption");
        tlsCheckbox.setSelected(true);

        Button connectButton = new Button("Connect to Server");
        connectButton.getStyleClass().addAll("button", "primary");
        connectButton.setPrefWidth(200);
        connectButton.setDefaultButton(true);

        Label statusLabel = new Label();
        statusLabel.getStyleClass().add("status-error");

        connectButton.setOnAction(e -> {
            String serverAddress = serverField.getText().trim();
            String nickname = nicknameField.getText().trim();
            String roomSecret = roomField.getText().trim();
            boolean useTls = tlsCheckbox.isSelected();

            String error = Validator.validateNickname(nickname);
            if (error != null) {
                statusLabel.getStyleClass().setAll("status-error");
                statusLabel.setText(error);
                return;
            }
            error = Validator.validateRoomSecret(roomSecret);
            if (error != null) {
                statusLabel.getStyleClass().setAll("status-error");
                statusLabel.setText(error);
                return;
            }

            connectButton.setDisable(true);
            statusLabel.getStyleClass().setAll("status-info");
            statusLabel.setText("Connecting...");
            callback.onConnect(nickname, roomSecret, useTls, serverAddress);
        });

        VBox card = new VBox(12);
        card.getStyleClass().add("login-card");
        card.setAlignment(Pos.CENTER);
        card.getChildren().addAll(titleLabel, subtitleLabel, sep,
                serverLabel, serverField,
                nicknameLabel, nicknameField,
                roomLabel, roomField,
                tlsCheckbox, connectButton, statusLabel);

        StackPane root = new StackPane(card);
        root.getStyleClass().add("login-root");
        StackPane.setAlignment(card, Pos.CENTER);
        StackPane.setMargin(card, new Insets(20));

        Scene scene = new Scene(root, 440, 520);
        Styles.apply(scene);
        stage.setTitle("LANIM - Login");
        stage.setMinWidth(400);
        stage.setMinHeight(480);
        return scene;
    }
}
