package com.fxdb.db.jdbc.dialect;

public class OracleDialect extends AbstractSqlDialect {
    @Override
    protected char quoteChar() {
        return '"';
    }

    @Override
    public boolean supportsIfNotExists() {
        // Oracle's CREATE TABLE has no IF NOT EXISTS clause; callers must guard via existence check.
        return false;
    }
}
