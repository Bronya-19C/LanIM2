package com.alpha.lanim.ui;

import javafx.scene.Scene;

public final class Styles {

    private static final String STYLESHEET =
            Styles.class.getResource("/styles/lanim.css").toExternalForm();

    private Styles() {
    }

    public static void apply(Scene scene) {
        scene.getStylesheets().add(STYLESHEET);
    }
}
