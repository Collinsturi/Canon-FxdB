package com.fxdb.db.jdbc.dialect;

public class MySqlDialect extends AbstractSqlDialect {
    @Override
    protected char quoteChar() {
        return '`';
    }
}
