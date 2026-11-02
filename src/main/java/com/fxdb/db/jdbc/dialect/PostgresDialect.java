package com.fxdb.db.jdbc.dialect;

public class PostgresDialect extends AbstractSqlDialect {
    @Override
    protected char quoteChar() {
        return '"';
    }
}
