package com.fxdb.ui.onboarding;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;

/**
 * A small step-through card over the main window: Next/Back to step through, dots to show
 * where you are, and either the X or Skip to dismiss early. Deliberately a separate modal
 * Stage rather than an overlay baked into Navigator's scene graph, since the tour describes
 * the whole wizard rather than any one screen and that way doesn't need to know about
 * Navigator's internals at all.
 */
public final class OnboardingTour {

    private OnboardingTour() {
    }

    public static void show(Stage owner, List<TourStep> steps, Runnable onFinished) {
        if (steps.isEmpty()) {
            onFinished.run();
            return;
        }

        Stage dialog = new Stage(StageStyle.UNDECORATED);
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);

        int[] currentStep = {0};

        Label titleLabel = new Label();
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label descriptionLabel = new Label();
        descriptionLabel.setWrapText(true);
        descriptionLabel.setMaxWidth(380);

        HBox dots = new HBox(6);
        dots.setAlignment(Pos.CENTER);

        Button backButton = new Button("Back");
        Button nextButton = new Button("Next");
        Button skipButton = new Button("Skip tour");

        Runnable[] render = new Runnable[1];
        render[0] = () -> {
            TourStep step = steps.get(currentStep[0]);
            titleLabel.setText(step.title());
            descriptionLabel.setText(step.description());
            backButton.setDisable(currentStep[0] == 0);
            nextButton.setText(currentStep[0] == steps.size() - 1 ? "Done" : "Next");

            dots.getChildren().clear();
            for (int i = 0; i < steps.size(); i++) {
                Circle dot = new Circle(4);
                dot.setFill(i == currentStep[0] ? Color.web("#2d7dd2") : Color.web("#cccccc"));
                dots.getChildren().add(dot);
            }
        };
        render[0].run();

        backButton.setOnAction(e -> {
            if (currentStep[0] > 0) {
                currentStep[0]--;
                render[0].run();
            }
        });
        nextButton.setOnAction(e -> {
            if (currentStep[0] < steps.size() - 1) {
                currentStep[0]++;
                render[0].run();
            } else {
                dialog.close();
            }
        });
        skipButton.setOnAction(e -> dialog.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox buttonRow = new HBox(10, backButton, spacer, skipButton, nextButton);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(14, titleLabel, descriptionLabel, dots, buttonRow);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; -fx-border-color: #cccccc; -fx-border-width: 1px;");
        card.setPrefWidth(420);

        StackPane root = new StackPane(card);
        root.setPadding(new Insets(1));

        dialog.setOnHidden(e -> onFinished.run());
        dialog.setScene(new Scene(root));
        dialog.show();

        dialog.setX(owner.getX() + (owner.getWidth() - card.getPrefWidth()) / 2);
        dialog.setY(owner.getY() + owner.getHeight() / 3);
    }
}
