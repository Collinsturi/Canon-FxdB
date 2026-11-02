package com.fxdb.ui.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** One row in the table-selection screen's checklist: a source table/sheet and the target name it'll be written as. */
public class TableSelectionRow {

    private final String sourceTable;
    private final BooleanProperty selected = new SimpleBooleanProperty(true);
    private final StringProperty targetTable;

    public TableSelectionRow(String sourceTable) {
        this.sourceTable = sourceTable;
        this.targetTable = new SimpleStringProperty(sourceTable);
    }

    public String getSourceTable() {
        return sourceTable;
    }

    public boolean isSelected() {
        return selected.get();
    }

    public BooleanProperty selectedProperty() {
        return selected;
    }

    public String getTargetTable() {
        return targetTable.get();
    }

    public void setTargetTable(String value) {
        targetTable.set(value);
    }

    public StringProperty targetTableProperty() {
        return targetTable;
    }
}
