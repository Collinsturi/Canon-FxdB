package com.fxdb.ui.controller;

import com.fxdb.transfer.MigrationRunResult;
import com.fxdb.transfer.TableTransferResult;
import com.fxdb.ui.MigrationWizardContext;
import com.fxdb.ui.Navigator;
import com.fxdb.ui.WizardScreen;
import com.fxdb.ui.model.TableTransferResultRow;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.stream.Collectors;

public class SummaryReportController implements WizardScreen {

    @FXML private Label headlineLabel;
    @FXML private TableView<TableTransferResultRow> resultsTable;
    @FXML private TableColumn<TableTransferResultRow, String> tableNameColumn;
    @FXML private TableColumn<TableTransferResultRow, Number> rowsReadColumn;
    @FXML private TableColumn<TableTransferResultRow, Number> rowsWrittenColumn;
    @FXML private TableColumn<TableTransferResultRow, Number> rowsSkippedColumn;
    @FXML private TableColumn<TableTransferResultRow, String> statusColumn;
    @FXML private TableColumn<TableTransferResultRow, Number> errorCountColumn;
    @FXML private TextArea errorsArea;

    private Navigator navigator;
    private MigrationRunResult runResult;

    @Override
    public void init(MigrationWizardContext context, Navigator navigator) {
        this.navigator = navigator;
        this.runResult = context.getLastRunResult();
        populate();
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
    }

    private void populate() {
        if (runResult == null) {
            headlineLabel.setText("No migration has been run yet.");
            return;
        }
        List<TableTransferResultRow> rows = runResult.tableResults().stream()
                .map(TableTransferResultRow::new)
                .toList();
        resultsTable.setItems(FXCollections.observableArrayList(rows));

        headlineLabel.setText(runResult.tableResults().size() + " table(s) processed, "
                + runResult.totalRowsWritten() + " row(s) written"
                + (runResult.anyFailed() ? " - one or more tables FAILED" : ""));

        String errorText = runResult.tableResults().stream()
                .flatMap(r -> r.errors().stream())
                .collect(Collectors.joining(System.lineSeparator()));
        errorsArea.setText(errorText.isBlank() ? "(no errors)" : errorText);
    }

    @FXML
    private void onExportReport() {
        if (runResult == null) {
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export migration report");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text", "*.txt"));
        File file = chooser.showSaveDialog(errorsArea.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            Files.writeString(file.toPath(), buildReportText());
        } catch (IOException e) {
            new Alert(Alert.AlertType.ERROR, "Failed to export report: " + e.getMessage()).showAndWait();
        }
    }

    private String buildReportText() {
        StringBuilder sb = new StringBuilder();
        sb.append(headlineLabel.getText()).append(System.lineSeparator()).append(System.lineSeparator());
        for (TableTransferResult result : runResult.tableResults()) {
            sb.append(result.tableName()).append(": ")
                    .append(result.status()).append(", read=").append(result.rowsRead())
                    .append(", written=").append(result.rowsWritten())
                    .append(", skipped=").append(result.rowsSkipped())
                    .append(System.lineSeparator());
            for (String error : result.errors()) {
                sb.append("    ").append(error).append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    @FXML
    private void onDone() {
        navigator.restart();
    }
}
