package com.fxdb.ui;

import com.fxdb.core.exception.FxDbException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Swaps the wizard's five screens in and out of one Stage, handing each controller the
 * shared context. The Stage's Scene root is a persistent shell (stepper on top, current
 * screen in the center) rather than being replaced wholesale each navigation, so the
 * step indicator survives across screens.
 */
public class Navigator {

    private final Stage stage;
    private final MigrationWizardContext context = new MigrationWizardContext();
    private final WizardStepper stepper = new WizardStepper();
    private final BorderPane shell = new BorderPane();

    public Navigator(Stage stage) {
        this.stage = stage;
        shell.setTop(stepper);
    }

    public MigrationWizardContext context() {
        return context;
    }

    public void showConnectionSetup() {
        show("connection-setup.fxml", 0);
    }

    public void showTableSelection() {
        show("table-selection.fxml", 1);
    }

    public void showMappingGrid() {
        show("mapping-grid.fxml", 2);
    }

    public void showExecution() {
        show("execution.fxml", 3);
    }

    public void showSummary() {
        show("summary-report.fxml", 4);
    }

    public void restart() {
        context.reset();
        showConnectionSetup();
    }

    private void show(String fxmlFileName, int stepIndex) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFileName));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof WizardScreen screen) {
                screen.init(context, this);
            }
            stepper.setActiveStep(stepIndex);
            shell.setCenter(root);
            if (stage.getScene() == null) {
                stage.setScene(new Scene(shell, 1000, 680));
            }
            stage.show();
        } catch (IOException e) {
            throw new FxDbException("Failed to load screen " + fxmlFileName, e);
        }
    }
}
