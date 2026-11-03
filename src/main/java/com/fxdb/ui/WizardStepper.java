package com.fxdb.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * A persistent, non-clickable progress readout for the 5-screen wizard: steps before
 * the active one are marked done, the active one is highlighted, later ones are dimmed.
 * Built in code rather than FXML, same as OnboardingTour, since it's one simple
 * self-contained component rather than a screen with a controller.
 */
public final class WizardStepper extends HBox {

    private static final String[] STEP_LABELS = {
            "Connect", "Tables", "Mapping", "Run", "Report"
    };

    private static final Color DONE_COLOR = Color.web("#2d7dd2");
    private static final Color ACTIVE_COLOR = Color.web("#2d7dd2");
    private static final Color UPCOMING_COLOR = Color.web("#cccccc");

    private final Circle[] circles = new Circle[STEP_LABELS.length];
    private final Label[] labels = new Label[STEP_LABELS.length];

    public WizardStepper() {
        setSpacing(10);
        setAlignment(Pos.CENTER);
        setPadding(new Insets(10));

        for (int i = 0; i < STEP_LABELS.length; i++) {
            Circle circle = new Circle(10);
            Label number = new Label(String.valueOf(i + 1));
            number.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");
            StackPane badge = new StackPane(circle, number);

            Label label = new Label(STEP_LABELS[i]);

            circles[i] = circle;
            labels[i] = label;

            getChildren().add(new HBox(6, badge, label));
            if (i < STEP_LABELS.length - 1) {
                Region connector = new Region();
                connector.setPrefWidth(30);
                connector.setStyle("-fx-border-color: #cccccc; -fx-border-width: 0 0 1 0;");
                getChildren().add(connector);
            }
        }

        setActiveStep(0);
    }

    /** index is 0-based; steps before it render as done, it renders as active, after it as upcoming. */
    public void setActiveStep(int index) {
        for (int i = 0; i < circles.length; i++) {
            boolean done = i < index;
            boolean active = i == index;
            circles[i].setFill(done || active ? DONE_COLOR : UPCOMING_COLOR);
            labels[i].setStyle(active
                    ? "-fx-font-weight: bold; -fx-text-fill: " + toWeb(ACTIVE_COLOR) + ";"
                    : done
                        ? "-fx-text-fill: " + toWeb(DONE_COLOR) + ";"
                        : "-fx-text-fill: " + toWeb(UPCOMING_COLOR) + ";");
        }
    }

    private static String toWeb(Color color) {
        return String.format("#%02x%02x%02x",
                (int) (color.getRed() * 255),
                (int) (color.getGreen() * 255),
                (int) (color.getBlue() * 255));
    }
}
