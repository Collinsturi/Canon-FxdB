package com.fxdb.db.jdbc.dialect;

import com.fxdb.core.model.DataType;

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

    @Override
    public String sqlTypeName(DataType type) {
        // Oracle has no native boolean column type (pre-23c) - NUMBER(1) is the conventional stand-in.
        return type == DataType.BOOLEAN ? "NUMBER(1)" : super.sqlTypeName(type);
    }
}
