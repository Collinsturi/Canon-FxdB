package com.fxdb.file;

import com.fxdb.db.jdbc.JdbcConnectionConfig;
import com.fxdb.db.jdbc.JdbcDataSink;
import com.fxdb.file.csv.CsvDataSource;
import com.fxdb.transfer.TransferEngine;
import com.fxdb.transfer.TransferResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class CsvToSqliteSmokeTest {

    @Test
    void migratesCsvFixtureIntoSqlite(@TempDir Path tempDir) throws SQLException {
        Path csvFixture = Path.of("src/test/resources/fixtures/customers.csv");
        String targetUrl = "jdbc:sqlite:" + tempDir.resolve("target.db");

        CsvDataSource source = new CsvDataSource(FileConnectionConfig.csv(csvFixture));
        JdbcDataSink sink = new JdbcDataSink(new JdbcConnectionConfig(targetUrl, null, null), "customers");

        TransferResult result = new TransferEngine().run(source, sink);

        assertThat(result.rowsWritten()).isEqualTo(3);

        try (Connection conn = DriverManager.getConnection(targetUrl)) {
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT name, age FROM customers WHERE id = 2")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("name")).isEqualTo("Bob");
                assertThat(rs.getLong("age")).isEqualTo(25);
            }
        }
    }
}
