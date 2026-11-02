package com.fxdb.db.jdbc.dialect;

import com.fxdb.core.model.Column;
import com.fxdb.core.model.TableSchema;
import com.fxdb.db.jdbc.JdbcTypeMapper;

import java.util.stream.Collectors;

abstract class AbstractSqlDialect implements SqlDialect {

    protected abstract char quoteChar();

    @Override
    public String quoteIdentifier(String identifier) {
        char q = quoteChar();
        return q + identifier + q;
    }

    @Override
    public String createTableDdl(TableSchema schema) {
        String columnDefs = schema.columns().stream()
                .map(this::columnDdl)
                .collect(Collectors.joining(", "));

        String pk = "";
        if (schema.primaryKeyColumns() != null && !schema.primaryKeyColumns().isEmpty()) {
            String pkCols = schema.primaryKeyColumns().stream()
                    .map(this::quoteIdentifier)
                    .collect(Collectors.joining(", "));
            pk = ", PRIMARY KEY (" + pkCols + ")";
        }

        String ifNotExists = supportsIfNotExists() ? "IF NOT EXISTS " : "";
        return "CREATE TABLE " + ifNotExists + quoteIdentifier(schema.tableName())
                + " (" + columnDefs + pk + ")";
    }

    private String columnDdl(Column column) {
        String nullability = column.nullable() ? "" : " NOT NULL";
        return quoteIdentifier(column.name()) + " " + JdbcTypeMapper.toAnsiSqlTypeName(column.type()) + nullability;
    }
}
