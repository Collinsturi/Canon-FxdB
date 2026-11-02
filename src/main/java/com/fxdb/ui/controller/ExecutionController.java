package com.fxdb.ui.controller;

import com.fxdb.mapping.model.TableMapping;
import com.fxdb.transfer.ErrorPolicy;
import com.fxdb.transfer.TableJob;
import com.fxdb.transfer.TableTransferResult;
import com.fxdb.transfer.TransferService;
import com.fxdb.ui.MigrationWizardContext;
import com.fxdb.ui.Navigator;
import com.fxdb.ui.WizardScreen;
import com.fxdb.ui.model.TableTransferResultRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;

import java.util.ArrayList;
import java.util.List;

public class ExecutionController implements WizardScreen {

    @FXML private ComboBox<ErrorPolicy> errorPolicyBox;
    @FXML private Button startButton;
    @FXML private Button cancelButton;
    @FXML private ProgressBar progressBar;
    @FXML private Label messageLabel;
    @FXML private TableView<TableTransferResultRow> resultsTable;
    @FXML private TableColumn<TableTransferResultRow, String> tableNameColumn;
    @FXML private TableColumn<TableTransferResultRow, Number> rowsReadColumn;
    @FXML private TableColumn<TableTransferResultRow, Number> rowsWrittenColumn;
    @FXML private TableColumn<TableTransferResultRow, Number> rowsSkippedColumn;
    @FXML private TableColumn<TableTransferResultRow, String> statusColumn;
    @FXML private TableColumn<TableTransferResultRow, Number> errorCountColumn;
    @FXML private TextArea logArea;
    @FXML private Button backButton;
    @FXML private Button summaryButton;

    private MigrationWizardContext context;
    private Navigator navigator;
    private TransferService service;
    private final ObservableList<TableTransferResultRow> resultRows = FXCollections.observableArrayList();
    private final List<TableTransferResult> collectedResults = new ArrayList<>();

    @Override
    public void init(MigrationWizardContext context, Navigator navigator) {
        this.context = context;
        this.navigator = navigator;
        errorPolicyBox.setItems(FXCollections.observableArrayList(ErrorPolicy.values()));
        errorPolicyBox.getSelectionModel().select(context.getErrorPolicy());
    }

    @FXML
    private void initialize() {
        tableNameColumn.setCellValueFactory(r -> javafx.beans.binding.Bindings.createStringBinding(
                r.getValue()::getTableName));
        rowsReadColumn.setCellValueFactory(r -> javafx.beans.binding.Bindings.createLongBinding(
                r.getValue()::getRowsRead));
        rowsWrittenColumn.setCellValueFactory(r -> javafx.beans.binding.Bindings.createLongBinding(
                r.getValue()::getRowsWritten));
        rowsSkippedColumn.setCellValueFactory(r -> javafx.beans.binding.Bindings.createLongBinding(
                r.getValue()::getRowsSkipped));
        statusColumn.setCellValueFactory(r -> javafx.beans.binding.Bindings.createStringBinding(
                r.getValue()::getStatus));
        errorCountColumn.setCellValueFactory(r -> javafx.beans.binding.Bindings.createLongBinding(
                r.getValue()::getErrorCount));
        resultsTable.setItems(resultRows);
    }

    @FXML
    private void onStart() {
        context.setErrorPolicy(errorPolicyBox.getValue());
        resultRows.clear();
        collectedResults.clear();
        logArea.clear();

        List<TableJob> jobs = new ArrayList<>();
        for (TableMapping mapping : context.getTableMappings()) {
            var source = context.getSource().createDataSource(mapping.getSourceTable());
            var sink = context.getTarget().createDataSink(mapping.getTargetTable());
            jobs.add(new TableJob(source, sink, mapping));
        }

        service = new TransferService(jobs, context.getErrorPolicy(), context.getBatchSize(),
                result -> {
                    collectedResults.add(result);
                    resultRows.add(new TableTransferResultRow(result));
                },
                logLine -> logArea.appendText(logLine + System.lineSeparator()));

        progressBar.progressProperty().bind(service.progressProperty());
        messageLabel.textProperty().bind(service.messageProperty());

        service.setOnSucceeded(e -> onRunFinished());
        service.setOnCancelled(e -> onRunFinished());
        service.setOnFailed(e -> {
            logArea.appendText("Run failed: " + service.getException() + System.lineSeparator());
            onRunFinished();
        });

        startButton.setDisable(true);
        cancelButton.setDisable(false);
        backButton.setDisable(true);
        service.start();
    }

    private void onRunFinished() {
        progressBar.progressProperty().unbind();
        messageLabel.textProperty().unbind();
        cancelButton.setDisable(true);
        backButton.setDisable(false);
        summaryButton.setDisable(false);
        context.setLastRunResult(new com.fxdb.transfer.MigrationRunResult(List.copyOf(collectedResults)));
    }

    @FXML
    private void onCancel() {
        if (service != null) {
            service.requestCancel();
        }
    }

    @FXML
    private void onBack() {
        navigator.showMappingGrid();
    }

    @FXML
    private void onViewSummary() {
        if (context.getLastRunResult() == null) {
            new Alert(Alert.AlertType.WARNING, "Run the migration first.").showAndWait();
            return;
        }
        navigator.showSummary();
    }
}
