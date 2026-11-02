package com.fxdb.db.jdbc;

import com.fxdb.core.model.DataType;

import java.sql.Types;

/** Translates vendor-specific java.sql.Types codes into the unified DataType enum, and back to JDBC setters. */
public final class JdbcTypeMapper {

    private JdbcTypeMapper() {
    }

    public static DataType fromJdbcType(int jdbcType) {
        return switch (jdbcType) {
            case Types.BIT, Types.BOOLEAN -> DataType.BOOLEAN;
            case Types.TINYINT, Types.SMALLINT, Types.INTEGER -> DataType.INTEGER;
            case Types.BIGINT -> DataType.LONG;
            case Types.FLOAT, Types.REAL, Types.DOUBLE -> DataType.DOUBLE;
            case Types.NUMERIC, Types.DECIMAL -> DataType.DECIMAL;
            case Types.DATE -> DataType.DATE;
            case Types.TIME, Types.TIMESTAMP, Types.TIME_WITH_TIMEZONE, Types.TIMESTAMP_WITH_TIMEZONE ->
                    DataType.TIMESTAMP;
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> DataType.BYTES;
            case Types.CHAR, Types.VARCHAR, Types.LONGVARCHAR, Types.NCHAR, Types.NVARCHAR,
                 Types.LONGNVARCHAR, Types.CLOB -> DataType.STRING;
            default -> DataType.UNKNOWN;
        };
    }

    /** Standard SQL type name used when generating CREATE TABLE DDL for a target that doesn't exist yet. */
    public static String toAnsiSqlTypeName(DataType type) {
        return switch (type) {
            case STRING -> "VARCHAR(255)";
            case INTEGER -> "INTEGER";
            case LONG -> "BIGINT";
            case DOUBLE -> "DOUBLE PRECISION";
            case DECIMAL -> "DECIMAL(18,4)";
            case BOOLEAN -> "BOOLEAN";
            case DATE -> "DATE";
            case TIMESTAMP -> "TIMESTAMP";
            case BYTES -> "BLOB";
            case UNKNOWN -> "VARCHAR(255)";
        };
    }
}
