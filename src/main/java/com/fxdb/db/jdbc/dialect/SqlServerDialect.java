package com.fxdb.db.jdbc.dialect;

import com.fxdb.core.model.DataType;

import java.util.Set;

public class SqlServerDialect extends AbstractSqlDialect {
    @Override
    protected char quoteChar() {
        return '"';
    }

    @Override
    public String sqlTypeName(DataType type) {
        // SQL Server has none of these as ANSI SQLDialect's default assumes: no BOOLEAN (use
        // BIT), no DOUBLE PRECISION (use FLOAT), no BLOB (use VARBINARY(MAX)), and its own
        // TIMESTAMP means a row-version column, not a date/time value (use DATETIME2).
        return switch (type) {
            case BOOLEAN -> "BIT";
            case DOUBLE -> "FLOAT";
            case BYTES -> "VARBINARY(MAX)";
            case TIMESTAMP -> "DATETIME2";
            default -> super.sqlTypeName(type);
        };
    }

    @Override
    public boolean supportsIfNotExists() {
        // SQL Server's CREATE TABLE has no IF NOT EXISTS clause; callers must guard via existence check.
        return false;
    }

    @Override
    public Set<String> systemSchemas() {
        // "sys" holds catalog views and DMVs (dm_broker_connection, database_role_members, etc.) -
        // every one of them shows up as a plain "VIEW" to DatabaseMetaData, same as a real view.
        return Set.of("sys", "information_schema");
    }

    @Override
    public boolean supportsDatabaseCreation() {
        return true;
    }

    @Override
    public String adminDatabase() {
        return "master";
    }

    @Override
    public String createDatabaseDdl(String databaseName) {
        return "CREATE DATABASE " + quoteIdentifier(databaseName);
    }
}
