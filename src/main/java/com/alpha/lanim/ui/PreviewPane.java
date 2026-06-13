package com.alpha.lanim.ui;

import javafx.beans.binding.Bindings;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class PreviewPane {

    private static final double PDF_DPI = 120;

    private final VBox root;
    private final Label titleLabel;
    private final StackPane previewArea;
    private final ScrollPane previewScroll;
    private final Label placeholderLabel;
    private final HBox pdfToolbar;
    private final Button pdfPrevButton;
    private final Button pdfNextButton;
    private final Label pdfPageLabel;
    private final ImageView pdfPageView;

    private PDDocument pdfDocument;
    private PDFRenderer pdfRenderer;
    private int pdfCurrentPage;
    private int pdfTotalPages;

    public PreviewPane() {
        titleLabel = new Label("File Preview");
        titleLabel.getStyleClass().add("pane-header");

        placeholderLabel = new Label("No preview yet");
        placeholderLabel.getStyleClass().add("preview-placeholder");

        previewArea = new StackPane(placeholderLabel);
        previewArea.getStyleClass().add("preview-area");
        previewArea.setAlignment(Pos.CENTER);
        previewArea.setMinHeight(200);

        previewScroll = new ScrollPane(previewArea);
        previewScroll.setFitToWidth(true);
        previewScroll.getStyleClass().add("preview-scroll");

        pdfPageView = new ImageView();
        pdfPageView.setPreserveRatio(true);
        pdfPageView.fitWidthProperty().bind(
                Bindings.max(previewScroll.widthProperty().subtract(32), 200));

        pdfPrevButton = new Button("Previous");
        pdfPrevButton.getStyleClass().addAll("button", "secondary", "preview-pdf-btn");
        pdfNextButton = new Button("Next");
        pdfNextButton.getStyleClass().addAll("button", "secondary", "preview-pdf-btn");
        pdfPageLabel = new Label("1 / 1");
        pdfPageLabel.getStyleClass().add("preview-page-label");
        pdfPageLabel.setMaxWidth(Double.MAX_VALUE);
        pdfPageLabel.setAlignment(Pos.CENTER);

        pdfPrevButton.setOnAction(e -> showPdfPage(pdfCurrentPage - 1));
        pdfNextButton.setOnAction(e -> showPdfPage(pdfCurrentPage + 1));

        pdfToolbar = new HBox(8, pdfPrevButton, pdfPageLabel, pdfNextButton);
        pdfToolbar.getStyleClass().add("preview-pdf-toolbar");
        pdfToolbar.setAlignment(Pos.CENTER);
        pdfToolbar.setVisible(false);
        pdfToolbar.setManaged(false);
        HBox.setHgrow(pdfPageLabel, Priority.ALWAYS);

        root = new VBox(8);
        root.getStyleClass().addAll("sidebar", "sidebar-right");
        root.getChildren().addAll(titleLabel, previewScroll, pdfToolbar);
        VBox.setVgrow(previewScroll, Priority.ALWAYS);
    }

    public Node getNode() {
        return root;
    }

    public void showFile(Path localPath, String contentType, String fileName) {
        closePdfDocument();
        hidePdfToolbar();
        previewArea.getChildren().clear();

        String name = fileName != null ? fileName
                : (localPath != null ? localPath.getFileName().toString() : "file");
        titleLabel.setText(name);

        if (localPath == null || !Files.exists(localPath)) {
            previewArea.getChildren().add(hintLabel("File not found"));
            return;
        }

        String resolvedType = resolveContentType(contentType, name);

        if (isImage(resolvedType, name)) {
            showImage(localPath);
            return;
        }

        if (isPdf(resolvedType, name)) {
            showPdf(localPath);
            return;
        }

        showUnsupported(localPath, name);
    }

    public void clear() {
        closePdfDocument();
        hidePdfToolbar();
        titleLabel.setText("File Preview");
        previewArea.getChildren().clear();
        previewArea.getChildren().add(placeholderLabel);
    }

    public static boolean canPreview(String contentType, String fileName) {
        String resolved = resolveContentType(contentType, fileName);
        return isImage(resolved, fileName) || isPdf(resolved, fileName);
    }

    private void showImage(Path localPath) {
        try (InputStream in = Files.newInputStream(localPath)) {
            Image image = new Image(in);
            if (image.isError()) {
                previewArea.getChildren().add(hintLabel("Failed to load image"));
                return;
            }
            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);
            imageView.fitWidthProperty().bind(
                    Bindings.max(previewScroll.widthProperty().subtract(32), 200));
            VBox wrapper = new VBox(imageView);
            wrapper.setAlignment(Pos.TOP_CENTER);
            wrapper.setPadding(new Insets(8, 0, 8, 0));
            previewArea.getChildren().add(wrapper);
        } catch (IOException e) {
            previewArea.getChildren().add(hintLabel("Failed to read image: " + e.getMessage()));
        }
    }

    private void showPdf(Path localPath) {
        try {
            pdfDocument = Loader.loadPDF(localPath.toFile());
            pdfTotalPages = pdfDocument.getNumberOfPages();
            if (pdfTotalPages == 0) {
                closePdfDocument();
                previewArea.getChildren().add(hintLabel("PDF has no pages"));
                return;
            }
            pdfRenderer = new PDFRenderer(pdfDocument);
            pdfCurrentPage = 0;

            VBox wrapper = new VBox(pdfPageView);
            wrapper.setAlignment(Pos.TOP_CENTER);
            wrapper.setPadding(new Insets(8, 0, 8, 0));
            previewArea.getChildren().add(wrapper);

            showPdfToolbar();
            showPdfPage(0);
        } catch (IOException | RuntimeException e) {
            closePdfDocument();
            hidePdfToolbar();
            previewArea.getChildren().add(hintLabel("PDF preview failed: " + e.getMessage()));
        }
    }

    private void showPdfPage(int pageIndex) {
        if (pdfRenderer == null || pdfTotalPages == 0) {
            return;
        }
        int page = Math.max(0, Math.min(pageIndex, pdfTotalPages - 1));
        pdfCurrentPage = page;
        try {
            BufferedImage rendered = pdfRenderer.renderImageWithDPI(page, (float) PDF_DPI);
            pdfPageView.setImage(SwingFXUtils.toFXImage(rendered, null));
            pdfPageLabel.setText((page + 1) + " / " + pdfTotalPages);
            pdfPrevButton.setDisable(page <= 0);
            pdfNextButton.setDisable(page >= pdfTotalPages - 1);
        } catch (IOException e) {
            pdfPageView.setImage(null);
            pdfPageLabel.setText("Error");
        }
    }

    private void showUnsupported(Path localPath, String fileName) {
        VBox card = new VBox(10);
        card.getStyleClass().add("preview-unsupported-card");
        card.setAlignment(Pos.CENTER);
        card.setMaxWidth(280);

        Label badge = new Label(fileTypeBadge(fileName));
        badge.getStyleClass().add("preview-file-badge");

        Label nameLabel = new Label(fileName);
        nameLabel.getStyleClass().add("preview-file-name");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(240);

        Label metaLabel = new Label(formatFileSize(localPath));
        metaLabel.getStyleClass().add("preview-file-meta");

        Label hint = new Label("This format cannot be previewed here.");
        hint.getStyleClass().add("preview-unsupported-hint");
        hint.setWrapText(true);
        hint.setMaxWidth(240);

        Button openButton = new Button("Open with default app");
        openButton.getStyleClass().addAll("button", "primary");
        openButton.setMaxWidth(Double.MAX_VALUE);
        openButton.setOnAction(e -> openExternally(localPath));

        card.getChildren().addAll(badge, nameLabel, metaLabel, hint, openButton);
        previewArea.getChildren().add(card);
    }

    private void showPdfToolbar() {
        pdfToolbar.setVisible(true);
        pdfToolbar.setManaged(true);
    }

    private void hidePdfToolbar() {
        pdfToolbar.setVisible(false);
        pdfToolbar.setManaged(false);
        pdfPageView.setImage(null);
    }

    private void closePdfDocument() {
        if (pdfDocument != null) {
            try {
                pdfDocument.close();
            } catch (IOException ignored) {
            }
            pdfDocument = null;
            pdfRenderer = null;
            pdfTotalPages = 0;
            pdfCurrentPage = 0;
        }
    }

    private static void openExternally(Path localPath) {
        if (!Desktop.isDesktopSupported()) {
            return;
        }
        try {
            Desktop.getDesktop().open(localPath.toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String fileTypeBadge(String fileName) {
        if (fileName == null) {
            return "FILE";
        }
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0 && dot < fileName.length() - 1) {
            String ext = fileName.substring(dot + 1).toUpperCase();
            return ext.length() > 8 ? ext.substring(0, 8) : ext;
        }
        return "FILE";
    }

    private static String formatFileSize(Path path) {
        try {
            long bytes = Files.size(path);
            if (bytes < 1024) {
                return bytes + " B";
            }
            if (bytes < 1024 * 1024) {
                return (bytes / 1024) + " KB";
            }
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        } catch (IOException e) {
            return "";
        }
    }

    public static String resolveContentType(String contentType, String fileName) {
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }
        if (fileName == null) {
            return null;
        }
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lower.endsWith(".gif")) {
            return "image/gif";
        }
        if (lower.endsWith(".webp")) {
            return "image/webp";
        }
        if (lower.endsWith(".bmp")) {
            return "image/bmp";
        }
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        return null;
    }

    private static boolean isImage(String contentType, String fileName) {
        if (contentType != null && contentType.toLowerCase().startsWith("image/")) {
            return true;
        }
        return hasExtension(fileName, ".png", ".jpg", ".jpeg", ".gif", ".webp", ".bmp");
    }

    private static boolean isPdf(String contentType, String fileName) {
        if (contentType != null && contentType.toLowerCase().contains("pdf")) {
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

    private static Label hintLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("preview-hint");
        return label;
    }
}
