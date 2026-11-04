package com.fxdb.db.jdbc.dialect;

import com.fxdb.core.model.DataType;

import java.util.Set;

public class PostgresDialect extends AbstractSqlDialect {
    @Override
    protected char quoteChar() {
        return '"';
    }

    @Override
    public String sqlTypeName(DataType type) {
        // Postgres has no BLOB type - binary data is BYTEA.
        return type == DataType.BYTES ? "BYTEA" : super.sqlTypeName(type);
    }

    @Override
    public Set<String> systemSchemas() {
        return Set.of("pg_catalog", "information_schema");
    }

    @Override
    public boolean supportsDatabaseCreation() {
        return true;
    }

    @Override
    public String adminDatabase() {
        // "postgres" is the maintenance database every install has, used precisely for
        // connecting before the real target database exists yet.
        return "postgres";
    }

    @Override
    public String createDatabaseDdl(String databaseName) {
        return "CREATE DATABASE " + quoteIdentifier(databaseName);
    }
}
