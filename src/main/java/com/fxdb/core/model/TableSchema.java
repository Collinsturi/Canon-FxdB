package com.fxdb.core.model;

import java.util.List;

public record TableSchema(
        String catalog,
        String schemaName,
        String tableName,
        List<Column> columns,
        List<String> primaryKeyColumns,
        List<ForeignKeyRef> foreignKeys
) {
    public Column column(String name) {
        return columns.stream()
                .filter(c -> c.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}
