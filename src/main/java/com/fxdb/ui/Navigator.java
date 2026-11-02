package com.fxdb.ui;

import com.fxdb.core.exception.FxDbException;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/** Swaps the wizard's five screens in and out of one Stage, handing each controller the shared context. */
public class Navigator {

    private final Stage stage;
    private final MigrationWizardContext context = new MigrationWizardContext();

    public Navigator(Stage stage) {
        this.stage = stage;
    }

    public MigrationWizardContext context() {
        return context;
    }

    public void showConnectionSetup() {
        show("connection-setup.fxml");
    }

    public void showTableSelection() {
        show("table-selection.fxml");
    }

    public void showMappingGrid() {
        show("mapping-grid.fxml");
    }

    public void showExecution() {
        show("execution.fxml");
    }

    public void showSummary() {
        show("summary-report.fxml");
    }

    public void restart() {
        context.reset();
        showConnectionSetup();
    }

    private void show(String fxmlFileName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFileName));
            Parent root = loader.load();
            Object controller = loader.getController();
            if (controller instanceof WizardScreen screen) {
                screen.init(context, this);
            }
            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root, 1000, 680));
            } else {
                scene.setRoot(root);
            }
            stage.show();
        } catch (IOException e) {
            throw new FxDbException("Failed to load screen " + fxmlFileName, e);
        }
    }
}
