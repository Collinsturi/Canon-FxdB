package com.fxdb.db.jdbc.dialect;

import com.fxdb.core.model.DataType;
import com.fxdb.core.model.TableSchema;
import com.fxdb.db.jdbc.JdbcTypeMapper;

import java.util.Set;

/** Isolates vendor-specific SQL quirks so JdbcDataSource/Sink stay vendor-neutral. */
public interface SqlDialect {

    String quoteIdentifier(String identifier);

    String createTableDdl(TableSchema schema);

    /**
     * The column type name to use in generated DDL. Defaults to {@link JdbcTypeMapper}'s ANSI
     * name, which several vendors don't actually accept as-is - e.g. SQL Server has neither
     * {@code BOOLEAN} nor {@code DOUBLE PRECISION} nor {@code BLOB}, and its own {@code TIMESTAMP}
     * means a row-version column, not a date/time value. Override per vendor where the ANSI name
     * isn't valid.
     */
    default String sqlTypeName(DataType type) {
        return JdbcTypeMapper.toAnsiSqlTypeName(type);
    }

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

    /** True for vendors where a brand-new database is something fxDb can create via plain SQL. */
    default boolean supportsDatabaseCreation() {
        return false;
    }

    /**
     * The database to connect to in order to issue {@link #createDatabaseDdl}, since you can't
     * create a database from a connection to that same database before it exists - e.g.
     * Postgres's {@code postgres} maintenance database, SQL Server's {@code master}. Empty
     * string for a vendor that allows connecting without selecting any database at all (MySQL).
     */
    default String adminDatabase() {
        throw new UnsupportedOperationException("Database creation isn't supported for this vendor");
    }

    default String createDatabaseDdl(String databaseName) {
        throw new UnsupportedOperationException("Database creation isn't supported for this vendor");
    }
}
