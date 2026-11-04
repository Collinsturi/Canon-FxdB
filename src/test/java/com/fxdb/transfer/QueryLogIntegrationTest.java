package com.fxdb.transfer;

import com.fxdb.core.QueryLog;
import com.fxdb.db.jdbc.JdbcConnectionConfig;
import com.fxdb.db.jdbc.JdbcDataSink;
import com.fxdb.db.jdbc.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Proves a real migration actually publishes to QueryLog - the SELECT, the CREATE TABLE DDL, and the INSERT batch. */
class QueryLogIntegrationTest {

    @AfterEach
    void clearListener() {
        QueryLog.setListener(null);
    }

    @Test
    void aSqliteTransferRecordsTheSelectCreateTableAndInsertBatch(@TempDir Path tempDir) throws SQLException {
        String sourceUrl = "jdbc:sqlite:" + tempDir.resolve("source.db");
        String targetUrl = "jdbc:sqlite:" + tempDir.resolve("target.db");

        try (Connection conn = DriverManager.getConnection(sourceUrl);
             Statement statement = conn.createStatement()) {
            statement.execute("CREATE TABLE customers (id INTEGER PRIMARY KEY, name TEXT NOT NULL)");
            statement.execute("INSERT INTO customers (id, name) VALUES (1, 'Alice')");
            statement.execute("INSERT INTO customers (id, name) VALUES (2, 'Bob')");
        }

        List<QueryLog.Entry> recorded = new ArrayList<>();
        QueryLog.setListener(recorded::add);

        JdbcConnectionConfig sourceConfig = new JdbcConnectionConfig(sourceUrl, null, null);
        JdbcConnectionConfig targetConfig = new JdbcConnectionConfig(targetUrl, null, null);
        new TransferEngine().run(new JdbcDataSource(sourceConfig, "customers"), new JdbcDataSink(targetConfig, "customers"));

        assertThat(recorded).extracting(QueryLog.Entry::sql)
                .anyMatch(sql -> sql.equals("SELECT * FROM customers"))
                .anyMatch(sql -> sql.startsWith("CREATE TABLE"))
                .anyMatch(sql -> sql.startsWith("INSERT INTO") && sql.contains("batch of 2 row(s)"));
        assertThat(recorded).allMatch(e -> e.jdbcUrl().equals(sourceUrl) || e.jdbcUrl().equals(targetUrl));

        assertThat(recorded)
                .filteredOn(e -> e.sql().equals("SELECT * FROM customers"))
                .allMatch(e -> e.role() == QueryLog.Role.SOURCE && e.jdbcUrl().equals(sourceUrl));
        assertThat(recorded)
                .filteredOn(e -> e.sql().startsWith("CREATE TABLE") || e.sql().startsWith("INSERT INTO"))
                .allMatch(e -> e.role() == QueryLog.Role.TARGET && e.jdbcUrl().equals(targetUrl));
    }
}
