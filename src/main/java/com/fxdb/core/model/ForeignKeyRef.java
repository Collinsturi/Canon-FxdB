package com.fxdb.core.model;

public record ForeignKeyRef(String fkColumn, String referencedTable, String referencedColumn) {
}
