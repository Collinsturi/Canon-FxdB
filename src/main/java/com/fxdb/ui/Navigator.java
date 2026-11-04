package com.fxdb.ui;

import com.fxdb.core.QueryLog;
import com.fxdb.core.exception.FxDbException;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Swaps the wizard's five screens in and out of one Stage, handing each controller the
 * shared context. The Stage's Scene root is a persistent shell (stepper on top, current
 * screen plus a busy overlay in the center, a live query log on the bottom) rather than being
 * replaced wholesale each navigation, so the step indicator, overlay, and query log all
 * survive across screens.
 */
public class Navigator {

    /** Step index (0-based) from which the query log is always shown - Run and Report, the two
     * stages where a query has actually been fired against a real database. Earlier stages
     * (Connect/Tables/Mapping) only ever touch schema metadata, not real DMLs, so the log stays
     * hidden there unless the user asks to see it via {@link #toggleQueryLogVisibility()}. */
    private static final int QUERY_LOG_FORCED_FROM_STEP = 3;

    private final Stage stage;
    private final MigrationWizardContext context = new MigrationWizardContext();
    private final WizardStepper stepper = new WizardStepper();
    private final LoadingOverlay overlay = new LoadingOverlay();
    private final StackPane contentStack = new StackPane();
    private final BorderPane shell = new BorderPane();
    private final QueryLogPanel queryLogPanel = new QueryLogPanel();
    private final Button queryLogToggle = new Button();

    private int currentStepIndex = 0;
    private boolean queryLogManuallyShown = false;

    public Navigator(Stage stage) {
        this.stage = stage;

        queryLogToggle.setOnAction(e -> toggleQueryLogVisibility());

        BorderPane topBar = new BorderPane();
        topBar.setCenter(stepper);
        topBar.setRight(queryLogToggle);

        shell.setTop(topBar);
        shell.setCenter(contentStack);
        shell.setBottom(queryLogPanel);
        QueryLog.setListener(entry -> Platform.runLater(() -> queryLogPanel.append(entry)));
        updateQueryLogVisibility();
    }

    public MigrationWizardContext context() {
        return context;
    }

    /** Shows the busy overlay over the current screen with the given message. */
    public void showBusy(String message) {
        overlay.show(message);
    }

    /** Hides the busy overlay. */
    public void hideBusy() {
        overlay.hide();
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
            contentStack.getChildren().setAll(root, overlay);
            stepper.setActiveStep(stepIndex);
            currentStepIndex = stepIndex;
            updateQueryLogVisibility();
            Object controller = loader.getController();
            if (controller instanceof WizardScreen screen) {
                screen.init(context, this);
            }
            if (stage.getScene() == null) {
                stage.setScene(new Scene(shell, 1000, 680));
            }
            stage.show();
        } catch (IOException e) {
            throw new FxDbException("Failed to load screen " + fxmlFileName, e);
        }
    }

    /** Lets a user override the query log's default hidden/shown state for the current stage. */
    public void toggleQueryLogVisibility() {
        queryLogManuallyShown = !queryLogManuallyShown;
        updateQueryLogVisibility();
    }

    private void updateQueryLogVisibility() {
        boolean forced = currentStepIndex >= QUERY_LOG_FORCED_FROM_STEP;
        boolean visible = forced || queryLogManuallyShown;
        queryLogPanel.setVisible(visible);
        queryLogPanel.setManaged(visible);
        queryLogToggle.setDisable(forced);
        queryLogToggle.setText(visible ? "Hide Query Log" : "Show Query Log");
    }
}
