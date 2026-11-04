package com.fxdb.ui;

import com.fxdb.core.QueryLog;
import com.fxdb.core.exception.FxDbException;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Swaps the wizard's five screens in and out of one Stage, handing each controller the
 * shared context. The Stage's Scene root is a persistent shell (stepper on top, current
 * screen plus a busy overlay in the center, a bottom tab-strip panel hosting the query log and
 * migration errors) rather than being replaced wholesale each navigation, so the step
 * indicator, overlay, and bottom tabs all survive across screens.
 */
public class Navigator {

    private static final String QUERY_LOG_TAB = "Query Log";
    private static final String ERRORS_TAB = "Errors";

    /** Step index (0-based) from which the query log is forced open - Run and Report, the two
     * stages where a query has actually been fired against a real database. Earlier stages
     * (Connect/Tables/Mapping) only ever touch schema metadata, not real DMLs, so the tab stays
     * collapsed by default there, but a user can still open it themselves by clicking it. */
    private static final int QUERY_LOG_FORCED_FROM_STEP = 3;

    private final Stage stage;
    private final MigrationWizardContext context = new MigrationWizardContext();
    private final WizardStepper stepper = new WizardStepper();
    private final LoadingOverlay overlay = new LoadingOverlay();
    private final StackPane contentStack = new StackPane();
    private final BorderPane shell = new BorderPane();
    private final BottomTabPanel bottomPanel = new BottomTabPanel();
    private final QueryLogView queryLogView = new QueryLogView();
    private final ErrorLogView errorLogView = new ErrorLogView();

    private int currentStepIndex = 0;
    private boolean queryLogWasForced = false;

    public Navigator(Stage stage) {
        this.stage = stage;

        bottomPanel.addTab(QUERY_LOG_TAB, queryLogView);
        bottomPanel.addTab(ERRORS_TAB, errorLogView);

        shell.setTop(stepper);
        shell.setCenter(contentStack);
        shell.setBottom(bottomPanel);
        QueryLog.setListener(entry -> Platform.runLater(() -> queryLogView.append(entry)));
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

    /**
     * Logs a migration-run error and, unless the user is already looking at the Errors tab,
     * switches focus to it - they can still freely switch back to Query Log afterward, this
     * only grabs focus on the moment a *new* error shows up.
     */
    public void reportError(String message) {
        errorLogView.append(message);
        if (!bottomPanel.isTabActive(ERRORS_TAB)) {
            bottomPanel.openTab(ERRORS_TAB);
        }
    }

    /** Clears the Errors tab's history - called at the start of a fresh migration run. */
    public void clearErrors() {
        errorLogView.clear();
    }

    private void updateQueryLogVisibility() {
        boolean forced = currentStepIndex >= QUERY_LOG_FORCED_FROM_STEP;
        bottomPanel.setTabPinned(QUERY_LOG_TAB, forced);
        // Only auto-open on the moment we enter the forced zone (Mapping -> Run), not on every
        // navigation within it - otherwise going Run -> Report would yank focus back from
        // whatever tab (e.g. Errors) the user was actually looking at.
        if (forced && !queryLogWasForced) {
            bottomPanel.openTab(QUERY_LOG_TAB);
        }
        queryLogWasForced = forced;
    }
}
