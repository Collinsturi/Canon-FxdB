package com.fxdb.db.jdbc.dialect;

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
}
