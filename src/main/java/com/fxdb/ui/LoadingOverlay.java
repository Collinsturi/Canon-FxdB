package com.fxdb.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * A full-area translucent overlay with a spinner and message, shown over the current
 * wizard screen while a background task (see BackgroundTasks) is in flight. Covers the
 * whole screen and intercepts clicks while visible, so Back/Next can't be pressed again
 * mid-request.
 */
public final class LoadingOverlay extends StackPane {

    private final Label messageLabel = new Label();

    public LoadingOverlay() {
        setStyle("-fx-background-color: rgba(255, 255, 255, 0.7);");
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(48, 48);
        messageLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold;");

        VBox card = new VBox(10, spinner, messageLabel);
        card.setAlignment(Pos.CENTER);
        getChildren().add(card);

        hide();
    }

    public void show(String message) {
        messageLabel.setText(message);
        setVisible(true);
        setManaged(true);
        toFront();
    }

    public void hide() {
        setVisible(false);
        setManaged(false);
    }
}
