package com.fxdb.core.model;

/** Unified type system that every DataSource/DataSink maps into and out of, independent of vendor. */
public enum DataType {
    STRING,
    INTEGER,
    LONG,
    DOUBLE,
    DECIMAL,
    BOOLEAN,
    DATE,
    TIMESTAMP,
    BYTES,
    UNKNOWN
}
