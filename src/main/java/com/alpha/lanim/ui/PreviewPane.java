package com.alpha.lanim.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;

import java.nio.file.Path;

public class PreviewPane {

    private final VBox root;
    private final Label titleLabel;
    private final StackPane previewArea;
    private final Label placeholderLabel;

    public PreviewPane() {
        titleLabel = new Label("File Preview");
        titleLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5 0 5 0;");

        placeholderLabel = new Label("No preview");
        placeholderLabel.setStyle("-fx-text-fill: #888;");

        previewArea = new StackPane(placeholderLabel);
        previewArea.setMinWidth(280);
        previewArea.setPrefWidth(320);
        previewArea.setAlignment(Pos.CENTER);
        previewArea.setStyle("-fx-background-color: #f5f5f5;");

        root = new VBox(5);
        root.setPadding(new Insets(10));
        root.getChildren().addAll(titleLabel, previewArea);
    }

    public Node getNode() {
        return root;
    }

    public void showFile(Path localPath, String contentType, String fileName) {
        previewArea.getChildren().clear();
        titleLabel.setText("Preview: " + (fileName != null ? fileName : localPath.getFileName()));

        if (localPath == null || !localPath.toFile().exists()) {
            previewArea.getChildren().add(unsupportedLabel("File not found"));
            return;
        }

        if (isImage(contentType, fileName)) {
            ImageView imageView = new ImageView(new Image(localPath.toUri().toString(), true));
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(300);
            previewArea.getChildren().add(imageView);
            return;
        }

        if (isPdf(contentType, fileName)) {
            WebView webView = new WebView();
            webView.setPrefWidth(300);
            webView.setPrefHeight(500);
            webView.getEngine().load(localPath.toUri().toString());
            previewArea.getChildren().add(webView);
            return;
        }

        previewArea.getChildren().add(unsupportedLabel("Preview not supported for this file type"));
    }

    public void clear() {
        titleLabel.setText("File Preview");
        previewArea.getChildren().clear();
        previewArea.getChildren().add(placeholderLabel);
    }

    public static boolean canPreview(String contentType, String fileName) {
        return isImage(contentType, fileName) || isPdf(contentType, fileName);
    }

    private static boolean isImage(String contentType, String fileName) {
        if (contentType != null && contentType.toLowerCase().startsWith("image/")) {
            return true;
        }
        return hasExtension(fileName, ".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp");
    }

    private static boolean isPdf(String contentType, String fileName) {
        if (contentType != null && contentType.equalsIgnoreCase("application/pdf")) {
            return true;
        }
        return hasExtension(fileName, ".pdf");
    }

    private static boolean hasExtension(String fileName, String... extensions) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        for (String ext : extensions) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    private static Label unsupportedLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setStyle("-fx-text-fill: #888; -fx-padding: 10;");
        return label;
    }
}
