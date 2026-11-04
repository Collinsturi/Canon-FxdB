package com.fxdb.db.jdbc;

import com.fxdb.core.QueryLog;
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

class SchemaCopierTest {

    @AfterEach
    void clearListener() {
        QueryLog.setListener(null);
    }

    @Test
    void copiesEveryTableAndSkipsOnesAlreadyPresent(@TempDir Path tempDir) throws SQLException {
        String sourceUrl = "jdbc:sqlite:" + tempDir.resolve("source.db");
        String targetUrl = "jdbc:sqlite:" + tempDir.resolve("target.db");

        try (Connection conn = DriverManager.getConnection(sourceUrl);
             Statement statement = conn.createStatement()) {
            statement.execute("CREATE TABLE customers (id INTEGER PRIMARY KEY, name TEXT NOT NULL)");
            statement.execute("CREATE TABLE orders (id INTEGER PRIMARY KEY, customer_id INTEGER)");
        }

        List<QueryLog.Entry> recorded = new ArrayList<>();
        QueryLog.setListener(recorded::add);

        JdbcConnectionConfig sourceConfig = new JdbcConnectionConfig(sourceUrl, null, null);
        JdbcConnectionConfig targetConfig = new JdbcConnectionConfig(targetUrl, null, null);

        int firstRun = SchemaCopier.copyAllTables(sourceConfig, targetConfig);
        assertThat(firstRun).isEqualTo(2);

        try (var provider = new JdbcSchemaProvider(targetConfig)) {
            assertThat(provider.listTables()).containsExactlyInAnyOrder("customers", "orders");
        }

        assertThat(recorded).extracting(QueryLog.Entry::sql)
                .anyMatch(sql -> sql.startsWith("CREATE TABLE") && sql.contains("customers"))
                .anyMatch(sql -> sql.startsWith("CREATE TABLE") && sql.contains("orders"));
        assertThat(recorded).allMatch(e -> e.role() == QueryLog.Role.TARGET && e.jdbcUrl().equals(targetUrl));

        recorded.clear();
        int secondRun = SchemaCopier.copyAllTables(sourceConfig, targetConfig);
        assertThat(secondRun).isZero();
        assertThat(recorded).isEmpty();
    }
}
