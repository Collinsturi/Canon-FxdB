package com.fxdb.transfer;

import com.fxdb.db.jdbc.JdbcConnectionConfig;
import com.fxdb.db.jdbc.JdbcDataSink;
import com.fxdb.db.jdbc.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class TransferEngineSqliteSmokeTest {

    @Test
    void migratesAllRowsFromSourceToTarget(@TempDir Path tempDir) throws SQLException {
        String sourceUrl = "jdbc:sqlite:" + tempDir.resolve("source.db");
        String targetUrl = "jdbc:sqlite:" + tempDir.resolve("target.db");

        seedSourceDatabase(sourceUrl);

        JdbcConnectionConfig sourceConfig = new JdbcConnectionConfig(sourceUrl, null, null);
        JdbcConnectionConfig targetConfig = new JdbcConnectionConfig(targetUrl, null, null);

        JdbcDataSource dataSource = new JdbcDataSource(sourceConfig, "customers");
        JdbcDataSink dataSink = new JdbcDataSink(targetConfig, "customers");

        TransferResult result = new TransferEngine().run(dataSource, dataSink);

        assertThat(result.rowsRead()).isEqualTo(3);
        assertThat(result.rowsWritten()).isEqualTo(3);

        try (Connection targetConn = DriverManager.getConnection(targetUrl);
             Statement statement = targetConn.createStatement()) {
            var rs = statement.executeQuery("SELECT COUNT(*) FROM customers");
            rs.next();
            assertThat(rs.getLong(1)).isEqualTo(3);

            var nameRs = statement.executeQuery("SELECT name FROM customers WHERE id = 2");
            nameRs.next();
            assertThat(nameRs.getString("name")).isEqualTo("Bob");
        }
    }

    private void seedSourceDatabase(String jdbcUrl) throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl);
             Statement statement = conn.createStatement()) {
            statement.execute("CREATE TABLE customers (id INTEGER PRIMARY KEY, name TEXT NOT NULL, age INTEGER)");
            statement.execute("INSERT INTO customers (id, name, age) VALUES (1, 'Alice', 30)");
            statement.execute("INSERT INTO customers (id, name, age) VALUES (2, 'Bob', 25)");
            statement.execute("INSERT INTO customers (id, name, age) VALUES (3, 'Carol', 40)");
        }
    }
}
