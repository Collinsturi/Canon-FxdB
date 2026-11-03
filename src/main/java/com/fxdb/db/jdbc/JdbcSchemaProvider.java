package com.fxdb.db.jdbc;

import com.fxdb.core.SchemaProvider;
import com.fxdb.core.exception.SchemaException;
import com.fxdb.core.model.Column;
import com.fxdb.core.model.ForeignKeyRef;
import com.fxdb.core.model.TableSchema;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class JdbcSchemaProvider implements SchemaProvider {

    private final JdbcConnectionConfig config;
    private Connection connection;
    private String catalog;

    public JdbcSchemaProvider(JdbcConnectionConfig config) {
        this.config = config;
    }

    private Connection connection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(config.jdbcUrl(), config.username(), config.password());
            } catch (SQLException e) {
                throw new SchemaException(
                        "Failed to connect to " + config.jdbcUrl() + ": " + JdbcErrors.describe(e), e);
            }
        }
        return connection;
    }

    /**
     * The connection's own catalog (database). Passed explicitly to every {@link DatabaseMetaData}
     * lookup below instead of {@code null} - per the JDBC spec, a {@code null} catalog means "match
     * every catalog visible to this login," which on servers where one login can see several sibling
     * databases (SQL Server and MySQL both allow this routinely) silently pulls in tables that don't
     * belong to the database the user actually connected to.
     */
    private String catalog() {
        if (catalog == null) {
            try {
                catalog = connection().getCatalog();
            } catch (SQLException e) {
                throw new SchemaException(
                        "Failed to determine catalog for " + config.jdbcUrl() + ": " + JdbcErrors.describe(e), e);
            }
        }
        return catalog;
    }

    @Override
    public List<String> listTables() {
        Set<String> systemSchemas = systemSchemasLowerCase();
        List<String> tables = new ArrayList<>();
        try {
            DatabaseMetaData metaData = connection().getMetaData();
            try (ResultSet rs = metaData.getTables(catalog(), null, "%", new String[]{"TABLE", "VIEW"})) {
                while (rs.next()) {
                    String schema = rs.getString("TABLE_SCHEM");
                    if (schema != null && systemSchemas.contains(schema.toLowerCase(Locale.ROOT))) {
                        continue;
                    }
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
        } catch (SQLException e) {
            throw new SchemaException("Failed to list tables: " + JdbcErrors.describe(e), e);
        }
        return tables;
    }

    private Set<String> systemSchemasLowerCase() {
        Set<String> schemas;
        try {
            schemas = DriverRegistry.fromJdbcUrl(config.jdbcUrl()).dialect().systemSchemas();
        } catch (IllegalArgumentException e) {
            // JDBC URL scheme isn't one of fxDb's supported providers (e.g. H2 in tests) -
            // nothing registered to filter by, so don't exclude anything.
            return Set.of();
        }
        Set<String> lowerCased = new HashSet<>();
        for (String schema : schemas) {
            lowerCased.add(schema.toLowerCase(Locale.ROOT));
        }
        return lowerCased;
    }

    @Override
    public TableSchema describeTable(String tableName) {
        try {
            DatabaseMetaData metaData = connection().getMetaData();
            List<Column> columns = readColumns(metaData, tableName);
            List<String> primaryKeys = readPrimaryKeys(metaData, tableName);
            List<ForeignKeyRef> foreignKeys = readForeignKeys(metaData, tableName);

            List<Column> withPk = columns.stream()
                    .map(c -> new Column(c.name(), c.type(), c.nullable(), c.precision(), c.scale(),
                            primaryKeys.contains(c.name())))
                    .toList();

            return new TableSchema(null, null, tableName, withPk, primaryKeys, foreignKeys);
        } catch (SQLException e) {
            throw new SchemaException(
                    "Failed to describe table " + tableName + ": " + JdbcErrors.describe(e), e);
        }
    }

    private List<Column> readColumns(DatabaseMetaData metaData, String tableName) throws SQLException {
        List<Column> columns = new ArrayList<>();
        try (ResultSet rs = metaData.getColumns(catalog(), null, tableName, "%")) {
            while (rs.next()) {
                String name = rs.getString("COLUMN_NAME");
                int jdbcType = rs.getInt("DATA_TYPE");
                boolean nullable = rs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls;
                int precision = rs.getInt("COLUMN_SIZE");
                int scale = rs.getInt("DECIMAL_DIGITS");
                columns.add(new Column(name, JdbcTypeMapper.fromJdbcType(jdbcType), nullable, precision, scale, false));
            }
        }
        return columns;
    }

    private List<String> readPrimaryKeys(DatabaseMetaData metaData, String tableName) throws SQLException {
        List<String> pk = new ArrayList<>();
        try (ResultSet rs = metaData.getPrimaryKeys(catalog(), null, tableName)) {
            while (rs.next()) {
                pk.add(rs.getString("COLUMN_NAME"));
            }
        }
        return pk;
    }

    private List<ForeignKeyRef> readForeignKeys(DatabaseMetaData metaData, String tableName) throws SQLException {
        List<ForeignKeyRef> fks = new ArrayList<>();
        try (ResultSet rs = metaData.getImportedKeys(catalog(), null, tableName)) {
            while (rs.next()) {
                fks.add(new ForeignKeyRef(
                        rs.getString("FKCOLUMN_NAME"),
                        rs.getString("PKTABLE_NAME"),
                        rs.getString("PKCOLUMN_NAME")));
            }
        }
        return fks;
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new SchemaException("Failed to close connection: " + JdbcErrors.describe(e), e);
            }
        }
    }
}
