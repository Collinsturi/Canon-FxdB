package com.fxdb.db.jdbc;

import com.fxdb.core.DataSource;
import com.fxdb.core.exception.TransferException;
import com.fxdb.core.model.Row;
import com.fxdb.core.model.RowBatch;
import com.fxdb.core.model.TableSchema;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class JdbcDataSource implements DataSource {

    private final JdbcConnectionConfig config;
    private final String tableName;

    private Connection connection;
    private TableSchema schema;

    public JdbcDataSource(JdbcConnectionConfig config, String tableName) {
        this.config = config;
        this.tableName = tableName;
    }

    @Override
    public void open() {
        try {
            connection = DriverManager.getConnection(config.jdbcUrl(), config.username(), config.password());
        } catch (SQLException e) {
            throw new TransferException("Failed to open source connection to " + config.jdbcUrl(), e);
        }
        try (JdbcSchemaProvider provider = new JdbcSchemaProvider(config)) {
            schema = provider.describeTable(tableName);
        }
    }

    @Override
    public TableSchema getSchema() {
        return schema;
    }

    @Override
    public long estimateRowCount() {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM " + tableName);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : -1;
        } catch (SQLException e) {
            return -1;
        }
    }

    @Override
    public Iterator<RowBatch> readBatches(int batchSize) {
        PreparedStatement statement;
        ResultSet resultSet;
        try {
            statement = connection.prepareStatement(
                    "SELECT * FROM " + tableName,
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY);
            statement.setFetchSize(batchSize);
            resultSet = statement.executeQuery();
        } catch (SQLException e) {
            throw new TransferException("Failed to query " + tableName, e);
        }
        return new ResultSetBatchIterator(resultSet, batchSize);
    }

    @Override
    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                throw new TransferException("Failed to close source connection", e);
            }
        }
    }

    /** Pulls batchSize rows at a time from a forward-only ResultSet until exhausted. */
    private static final class ResultSetBatchIterator implements Iterator<RowBatch> {
        private final ResultSet resultSet;
        private final int batchSize;
        private String[] columnNames;
        private boolean exhausted = false;
        private RowBatch nextBatch;

        ResultSetBatchIterator(ResultSet resultSet, int batchSize) {
            this.resultSet = resultSet;
            this.batchSize = batchSize;
            try {
                ResultSetMetaData meta = resultSet.getMetaData();
                columnNames = new String[meta.getColumnCount()];
                for (int i = 0; i < columnNames.length; i++) {
                    columnNames[i] = meta.getColumnLabel(i + 1);
                }
            } catch (SQLException e) {
                throw new TransferException("Failed to read result set metadata", e);
            }
            advance();
        }

        private void advance() {
            if (exhausted) {
                nextBatch = null;
                return;
            }
            List<Row> rows = new ArrayList<>(batchSize);
            try {
                while (rows.size() < batchSize && resultSet.next()) {
                    Row row = new Row();
                    for (String columnName : columnNames) {
                        row = row.with(columnName, resultSet.getObject(columnName));
                    }
                    rows.add(row);
                }
                if (rows.size() < batchSize) {
                    exhausted = true;
                    resultSet.close();
                }
            } catch (SQLException e) {
                throw new TransferException("Failed while reading rows", e);
            }
            nextBatch = rows.isEmpty() ? null : new RowBatch(rows);
        }

        @Override
        public boolean hasNext() {
            return nextBatch != null;
        }

        @Override
        public RowBatch next() {
            if (nextBatch == null) {
                throw new NoSuchElementException();
            }
            RowBatch batch = nextBatch;
            advance();
            return batch;
        }
    }
}
