package com.fxdb.ui.controller;

import com.fxdb.core.SchemaProvider;
import com.fxdb.core.model.DataType;
import com.fxdb.core.model.TableSchema;
import com.fxdb.mapping.MappingEngine;
import com.fxdb.mapping.MappingProfileSerializer;
import com.fxdb.mapping.model.ColumnMapping;
import com.fxdb.mapping.model.ColumnTransform;
import com.fxdb.mapping.model.MigrationProfile;
import com.fxdb.mapping.model.TableMapping;
import com.fxdb.ui.MigrationWizardContext;
import com.fxdb.ui.Navigator;
import com.fxdb.ui.WizardScreen;
import com.fxdb.ui.model.ColumnMappingRow;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MappingGridController implements WizardScreen {

    @FXML private TabPane tabPane;

    private MigrationWizardContext context;
    private Navigator navigator;

    @Override
    public void init(MigrationWizardContext context, Navigator navigator) {
        this.context = context;
        this.navigator = navigator;
        rebuildTabs();
    }

    private void rebuildTabs() {
        tabPane.getTabs().clear();
        for (TableMapping mapping : context.getTableMappings()) {
            tabPane.getTabs().add(buildTab(mapping));
        }
    }

    private Tab buildTab(TableMapping mapping) {
        TableSchema sourceSchema = describeSource(mapping.getSourceTable());

        TableView<ColumnMappingRow> table = new TableView<>();
        table.setEditable(true);

        TableColumn<ColumnMappingRow, Boolean> includedCol = new TableColumn<>("Include");
        includedCol.setCellValueFactory(r -> r.getValue().includedProperty());
        includedCol.setCellFactory(CheckBoxTableCell.forTableColumn(includedCol));
        includedCol.setPrefWidth(60);

        TableColumn<ColumnMappingRow, String> sourceCol = new TableColumn<>("Source Column");
        sourceCol.setCellValueFactory(r -> r.getValue().sourceColumnProperty());
        sourceCol.setPrefWidth(160);

        TableColumn<ColumnMappingRow, String> sourceTypeCol = new TableColumn<>("Source Type");
        sourceTypeCol.setCellValueFactory(r -> javafx.beans.binding.Bindings.createStringBinding(
                r.getValue()::getSourceType));
        sourceTypeCol.setPrefWidth(100);

        TableColumn<ColumnMappingRow, String> targetCol = new TableColumn<>("Target Column");
        targetCol.setCellValueFactory(r -> r.getValue().targetColumnProperty());
        targetCol.setCellFactory(TextFieldTableCell.forTableColumn());
        targetCol.setEditable(true);
        targetCol.setPrefWidth(160);

        TableColumn<ColumnMappingRow, DataType> targetTypeCol = new TableColumn<>("Target Type");
        targetTypeCol.setCellValueFactory(r -> r.getValue().targetTypeProperty());
        targetTypeCol.setCellFactory(ComboBoxTableCell.forTableColumn(DataType.values()));
        targetTypeCol.setEditable(true);
        targetTypeCol.setPrefWidth(100);

        TableColumn<ColumnMappingRow, ColumnTransform> transformCol = new TableColumn<>("Transform");
        transformCol.setCellValueFactory(r -> r.getValue().transformProperty());
        transformCol.setCellFactory(ComboBoxTableCell.forTableColumn(ColumnTransform.values()));
        transformCol.setEditable(true);
        transformCol.setPrefWidth(120);

        TableColumn<ColumnMappingRow, Number> confidenceCol = new TableColumn<>("Confidence");
        confidenceCol.setCellValueFactory(r -> r.getValue().matchConfidenceProperty());
        confidenceCol.setCellFactory(col -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(Number value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                double v = value.doubleValue();
                setText(String.format("%.0f%%", v * 100));
                setStyle(v < 0.5 ? "-fx-text-fill: #c0392b;" : "-fx-text-fill: #27ae60;");
            }
        });
        confidenceCol.setPrefWidth(90);

        table.getColumns().setAll(List.of(includedCol, sourceCol, sourceTypeCol, targetCol, targetTypeCol, transformCol, confidenceCol));
        table.setItems(toRows(mapping, sourceSchema));
        table.setUserData(mapping);

        Tab tab = new Tab(mapping.getSourceTable() + " -> " + mapping.getTargetTable(), table);
        tab.setClosable(false);
        return tab;
    }

    private javafx.collections.ObservableList<ColumnMappingRow> toRows(TableMapping mapping, TableSchema sourceSchema) {
        List<ColumnMappingRow> rows = new ArrayList<>();
        for (ColumnMapping columnMapping : mapping.getColumns()) {
            String sourceType = sourceSchema == null ? "" :
                    sourceSchema.columns().stream()
                            .filter(c -> c.name().equals(columnMapping.getSourceColumn()))
                            .findFirst()
                            .map(c -> c.type().name())
                            .orElse("");
            rows.add(new ColumnMappingRow(columnMapping, sourceType));
        }
        return FXCollections.observableArrayList(rows);
    }

    private TableSchema describeSource(String sourceTable) {
        try (SchemaProvider provider = context.getSource().createSchemaProvider()) {
            return provider.describeTable(sourceTable);
        } catch (Exception e) {
            return null;
        }
    }

    @FXML
    private void onAutoMatch() {
        try (SchemaProvider sourceProvider = context.getSource().createSchemaProvider();
             SchemaProvider targetProvider = context.getTarget().createSchemaProvider()) {

            List<String> existingTargetTables = targetProvider.listTables();
            List<TableMapping> refreshed = new ArrayList<>();
            for (TableMapping mapping : context.getTableMappings()) {
                TableSchema sourceSchema = sourceProvider.describeTable(mapping.getSourceTable());
                TableSchema targetSchema = existingTargetTables.contains(mapping.getTargetTable())
                        ? targetProvider.describeTable(mapping.getTargetTable())
                        : null;
                refreshed.add(MappingEngine.autoMatch(sourceSchema, mapping.getTargetTable(), targetSchema));
            }
            context.setTableMappings(refreshed);
            rebuildTabs();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Auto-match failed: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onSaveProfile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save migration profile");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File file = chooser.showSaveDialog(tabPane.getScene().getWindow());
        if (file == null) {
            return;
        }
        MigrationProfile profile = new MigrationProfile(
                context.getSource().toDescriptor(),
                context.getTarget().toDescriptor(),
                context.getTableMappings());
        try {
            MappingProfileSerializer.save(profile, file.toPath());
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Failed to save profile: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onLoadProfile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Load migration profile");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File file = chooser.showOpenDialog(tabPane.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            MigrationProfile profile = MappingProfileSerializer.load(file.toPath());
            context.setTableMappings(profile.getTableMappings());
            rebuildTabs();
        } catch (Exception e) {
            new Alert(Alert.AlertType.ERROR, "Failed to load profile: " + e.getMessage()).showAndWait();
        }
    }

    @FXML
    private void onBack() {
        navigator.showTableSelection();
    }

    @FXML
    private void onNext() {
        navigator.showExecution();
    }
}
