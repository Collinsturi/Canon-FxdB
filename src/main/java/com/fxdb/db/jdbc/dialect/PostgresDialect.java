package com.fxdb.db.jdbc.dialect;

import java.util.Set;

public class PostgresDialect extends AbstractSqlDialect {
    @Override
    protected char quoteChar() {
        return '"';
    }

    @Override
    public Set<String> systemSchemas() {
        return Set.of("pg_catalog", "information_schema");
    }
}
