package com.fxdb.db.jdbc.dialect;

import com.fxdb.core.model.TableSchema;

import java.util.Set;

/** Isolates vendor-specific SQL quirks so JdbcDataSource/Sink stay vendor-neutral. */
public interface SqlDialect {

    String quoteIdentifier(String identifier);

    String createTableDdl(TableSchema schema);

    default boolean supportsIfNotExists() {
        return true;
    }

    /**
     * Schema names (case-insensitive) that hold the vendor's own catalog views/system tables
     * rather than user data - e.g. SQL Server's {@code sys}, MySQL's {@code information_schema}.
     * {@link com.fxdb.db.jdbc.JdbcSchemaProvider#listTables} excludes tables in these schemas so
     * a schema's object list isn't swamped with hundreds of system objects a migration would
     * never target.
     */
    default Set<String> systemSchemas() {
        return Set.of();
    }
}
