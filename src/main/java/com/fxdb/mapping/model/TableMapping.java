package com.fxdb.mapping.model;

import com.fxdb.core.model.Column;
import com.fxdb.core.model.TableSchema;

import java.util.ArrayList;
import java.util.List;

public class TableMapping {

    private String sourceTable;
    private String targetTable;
    private List<ColumnMapping> columns = new ArrayList<>();
    private boolean enabled = true;

    public TableMapping() {
    }

    public TableMapping(String sourceTable, String targetTable, List<ColumnMapping> columns, boolean enabled) {
        this.sourceTable = sourceTable;
        this.targetTable = targetTable;
        this.columns = columns;
        this.enabled = enabled;
    }

    public String getSourceTable() {
        return sourceTable;
    }

    public void setSourceTable(String sourceTable) {
        this.sourceTable = sourceTable;
    }

    public String getTargetTable() {
        return targetTable;
    }

    public void setTargetTable(String targetTable) {
        this.targetTable = targetTable;
    }

    public List<ColumnMapping> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnMapping> columns) {
        this.columns = columns;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** The schema the target table should have, derived from the included column mappings alone. */
    public TableSchema toTargetSchema() {
        List<Column> targetColumns = columns.stream()
                .filter(ColumnMapping::isIncluded)
                .filter(c -> c.getTargetColumn() != null)
                .map(c -> new Column(c.getTargetColumn(), c.getTargetType(), true))
                .toList();
        return new TableSchema(null, null, targetTable, targetColumns, List.of(), List.of());
    }
}
