package com.fxdb.db.jdbc;

import com.fxdb.core.DataSink;
import com.fxdb.core.exception.TransferException;
import com.fxdb.core.model.Column;
import com.fxdb.core.model.Row;
import com.fxdb.core.model.RowBatch;
import com.fxdb.core.model.TableSchema;
import com.fxdb.db.jdbc.dialect.SqlDialect;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

public class JdbcDataSink implements DataSink {

    private final JdbcConnectionConfig config;
    private final String tableName;

    private Connection connection;
    private TableSchema targetSchema;
    private PreparedStatement insertStatement;

    public JdbcDataSink(JdbcConnectionConfig config, String tableName) {
        this.config = config;
        this.tableName = tableName;
    }

    @Override
    public void open(TableSchema targetSchema, boolean createIfMissing) {
        this.targetSchema = targetSchema;
        try {
            connection = DriverManager.getConnection(config.jdbcUrl(), config.username(), config.password());
            connection.setAutoCommit(false);
        } catch (SQLException e) {
            throw new TransferException(
                    "Failed to open target connection to " + config.jdbcUrl() + ": " + JdbcErrors.describe(e), e);
        }

        if (createIfMissing && !tableExists()) {
            createTable(targetSchema);
        }
    }

    private boolean tableExists() {
        try {
            // Pass the connection's own catalog explicitly - a null catalog matches every
            // database a login can see on the server, which can falsely report a same-named
            // table in a sibling database as already existing here.
            String catalog = connection.getCatalog();
            var tables = connection.getMetaData().getTables(catalog, null, tableName, new String[]{"TABLE"});
            boolean exists = tables.next();
            tables.close();
            return exists;
        } catch (SQLException e) {
            throw new TransferException(
                    "Failed to check existence of table " + tableName + ": " + JdbcErrors.describe(e), e);
        }
    }

    private void createTable(TableSchema schema) {
        SqlDialect dialect = DriverRegistry.fromJdbcUrl(config.jdbcUrl()).dialect();
        String ddl = dialect.createTableDdl(schema);
        try (Statement statement = connection.createStatement()) {
            statement.execute(ddl);
            connection.commit();
        } catch (SQLException e) {
            throw new TransferException("Failed to create target table " + tableName + " with DDL: " + ddl
                    + " - " + JdbcErrors.describe(e), e);
        }
    }

    @Override
    public void writeBatch(RowBatch batch) {
        if (batch.isEmpty()) {
            return;
        }
        ensureInsertStatement();
        try {
            for (Row row : batch.rows()) {
                int i = 1;
                for (Column column : targetSchema.columns()) {
                    insertStatement.setObject(i++, row.get(column.name()));
                }
                insertStatement.addBatch();
            }
            insertStatement.executeBatch();
        } catch (SQLException e) {
            throw new TransferException(
                    "Failed to write batch to " + tableName + ": " + JdbcErrors.describe(e), e);
        }
    }

    private void ensureInsertStatement() {
        if (insertStatement != null) {
            return;
        }
        SqlDialect dialect = DriverRegistry.fromJdbcUrl(config.jdbcUrl()).dialect();
        List<Column> columns = targetSchema.columns();
        String columnList = columns.stream()
                .map(c -> dialect.quoteIdentifier(c.name()))
                .collect(Collectors.joining(", "));
        String placeholders = columns.stream().map(c -> "?").collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + dialect.quoteIdentifier(tableName)
                + " (" + columnList + ") VALUES (" + placeholders + ")";
        try {
            insertStatement = connection.prepareStatement(sql);
        } catch (SQLException e) {
            throw new TransferException(
                    "Failed to prepare insert statement for " + tableName + ": " + JdbcErrors.describe(e), e);
        }
    }

    @Override
    public void commit() {
        try {
            connection.commit();
        } catch (SQLException e) {
            throw new TransferException(
                    "Failed to commit transfer to " + tableName + ": " + JdbcErrors.describe(e), e);
        }
    }

    @Override
    public void close() {
        try {
            if (insertStatement != null) {
                insertStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            throw new TransferException(
                    "Failed to close target connection: " + JdbcErrors.describe(e), e);
        }
    }
}
