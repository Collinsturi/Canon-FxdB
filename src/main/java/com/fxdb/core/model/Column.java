package com.fxdb.core.model;

public record Column(
        String name,
        DataType type,
        boolean nullable,
        Integer precision,
        Integer scale,
        boolean primaryKey
) {
    public Column(String name, DataType type, boolean nullable) {
        this(name, type, nullable, null, null, false);
    }
}
