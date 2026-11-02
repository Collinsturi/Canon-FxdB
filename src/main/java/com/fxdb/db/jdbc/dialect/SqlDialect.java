package com.fxdb.db.jdbc.dialect;

import com.fxdb.core.model.TableSchema;

/** Isolates vendor-specific SQL quirks so JdbcDataSource/Sink stay vendor-neutral. */
public interface SqlDialect {

    String quoteIdentifier(String identifier);

    String createTableDdl(TableSchema schema);

    default boolean supportsIfNotExists() {
        return true;
    }
}
