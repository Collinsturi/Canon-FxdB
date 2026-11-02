package com.fxdb.db.jdbc.dialect;

public class SqliteDialect extends AbstractSqlDialect {
    @Override
    protected char quoteChar() {
        return '"';
    }
}
