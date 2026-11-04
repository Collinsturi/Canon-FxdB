package com.fxdb.db.jdbc.dialect;

import java.util.Set;

public class MySqlDialect extends AbstractSqlDialect {
    @Override
    protected char quoteChar() {
        return '`';
    }

    @Override
    public Set<String> systemSchemas() {
        return Set.of("information_schema", "performance_schema", "mysql", "sys");
    }

    @Override
    public boolean supportsDatabaseCreation() {
        return true;
    }

    @Override
    public String adminDatabase() {
        // MySQL allows connecting with no database selected at all.
        return "";
    }

    @Override
    public String createDatabaseDdl(String databaseName) {
        return "CREATE DATABASE " + quoteIdentifier(databaseName);
    }
}
