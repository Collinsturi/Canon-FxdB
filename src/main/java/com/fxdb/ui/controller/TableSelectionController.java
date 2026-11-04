package com.fxdb.ui.controller;

import com.fxdb.core.SchemaProvider;
import com.fxdb.core.model.TableSchema;
import com.fxdb.mapping.MappingEngine;
import com.fxdb.mapping.model.TableMapping;
import com.fxdb.ui.BackgroundTasks;
import com.fxdb.ui.MigrationWizardContext;
import com.fxdb.ui.Navigator;
import com.fxdb.ui.WizardScreen;
import com.fxdb.ui.model.TableSelectionRow;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;

import java.util.ArrayList;
import java.util.List;

public class TableSelectionController implements WizardScreen {

    @FXML private TableView<TableSelectionRow> tableList;
    @FXML private TableColumn<TableSelectionRow, Boolean> selectedColumn;
    @FXML private TableColumn<TableSelectionRow, String> sourceTableColumn;
    @FXML private TableColumn<TableSelectionRow, String> targetTableColumn;
    @FXML private Label statusLabel;

    private MigrationWizardContext context;
    private Navigator navigator;

    @Override
    public void init(MigrationWizardContext context, Navigator navigator) {
        this.context = context;
        this.navigator = navigator;
        loadSourceTables();
    }

    @FXML
    private void initialize() {
        selectedColumn.setCellValueFactory(row -> row.getValue().selectedProperty());
        selectedColumn.setCellFactory(CheckBoxTableCell.forTableColumn(selectedColumn));

        sourceTableColumn.setCellValueFactory(row -> javafx.beans.binding.Bindings.createStringBinding(
                row.getValue()::getSourceTable));

        targetTableColumn.setCellValueFactory(row -> row.getValue().targetTableProperty());
        targetTableColumn.setCellFactory(TextFieldTableCell.forTableColumn());
    }

    private void loadSourceTables() {
        BackgroundTasks.run(navigator, "Loading source tables...",
                () -> {
                    try (SchemaProvider provider = context.getSource().createSchemaProvider()) {
                        List<TableSelectionRow> rows = new ArrayList<>();
                        for (String table : provider.listTables()) {
                            rows.add(new TableSelectionRow(table));
                        }
                        return rows;
                    }
                },
                rows -> {
                    tableList.setItems(FXCollections.observableArrayList(rows));
                    statusLabel.setText(rows.size() + " table(s)/sheet(s) found.");
                },
                ex -> {
                    statusLabel.setText("Failed to list source tables: " + ex.getMessage());
                    statusLabel.setStyle("-fx-text-fill: red;");
                });
    }

    @FXML
    private void onBack() {
        navigator.showConnectionSetup();
    }

    @FXML
    private void onNext() {
        List<TableSelectionRow> selected = tableList.getItems().stream()
                .filter(TableSelectionRow::isSelected)
                .toList();
        if (selected.isEmpty()) {
            new Alert(Alert.AlertType.WARNING, "Select at least one table.").showAndWait();
            return;
        }

        BackgroundTasks.run(navigator, "Preparing column mappings...",
                () -> {
                    List<TableMapping> mappings = new ArrayList<>();
                    try (SchemaProvider sourceProvider = context.getSource().createSchemaProvider();
                         SchemaProvider targetProvider = context.getTarget().createSchemaProvider()) {

                        List<String> existingTargetTables = targetProvider.listTables();

                        for (TableSelectionRow row : selected) {
                            TableSchema sourceSchema = sourceProvider.describeTable(row.getSourceTable());
                            String targetTableName = row.getTargetTable();
                            TableSchema targetSchema = existingTargetTables.contains(targetTableName)
                                    ? targetProvider.describeTable(targetTableName)
                                    : null;
                            mappings.add(MappingEngine.autoMatch(sourceSchema, targetTableName, targetSchema));
                        }
                    }
                    return mappings;
                },
                mappings -> {
                    context.setTableMappings(mappings);
                    navigator.showMappingGrid();
                },
                ex -> new Alert(Alert.AlertType.ERROR, "Failed to prepare mappings: " + ex.getMessage()).showAndWait());
    }
}
