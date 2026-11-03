package com.fxdb.db.jdbc.dialect;

import java.util.Set;

public class SqlServerDialect extends AbstractSqlDialect {
    @Override
    protected char quoteChar() {
        return '"';
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
}
