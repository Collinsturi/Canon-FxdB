package com.fxdb.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * A brief, undecorated splash shown while the app starts up. Dismisses itself automatically
 * after a short pause, or immediately on click for anyone who'd rather skip the wait.
 */
public final class SplashScreen {

    private static final Duration DISPLAY_DURATION = Duration.seconds(1.4);
    private static final Duration FADE_DURATION = Duration.millis(300);

    private SplashScreen() {
    }

    public static void show(Runnable onFinished) {
        Stage stage = new Stage(StageStyle.UNDECORATED);

        Label title = new Label("Canon FxdB");
        title.setStyle("-fx-font-size: 36px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label tagline = new Label("Database Migration Tool");
        tagline.setStyle("-fx-font-size: 14px; -fx-text-fill: #cccccc;");
        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(32, 32);

        VBox content = new VBox(12, title, tagline, progress);
        content.setAlignment(Pos.CENTER);

        StackPane root = new StackPane(content);
        root.setPrefSize(420, 260);
        root.setStyle("-fx-background-color: #1b2733;");

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.centerOnScreen();

        boolean[] finished = {false};
        Runnable finish = () -> {
            if (finished[0]) {
                return;
            }
            finished[0] = true;
            FadeTransition fadeOut = new FadeTransition(FADE_DURATION, root);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                stage.close();
                onFinished.run();
            });
            fadeOut.play();
        };

        root.setOnMouseClicked(e -> finish.run());

        PauseTransition pause = new PauseTransition(DISPLAY_DURATION);
        pause.setOnFinished(e -> finish.run());

        stage.show();
        pause.play();
    }
}
