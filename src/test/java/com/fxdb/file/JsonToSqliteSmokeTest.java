package com.fxdb.file;

import com.fxdb.db.jdbc.JdbcConnectionConfig;
import com.fxdb.db.jdbc.JdbcDataSink;
import com.fxdb.file.json.JsonDataSource;
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

class JsonToSqliteSmokeTest {

    @Test
    void migratesJsonFixtureIntoSqlite(@TempDir Path tempDir) throws SQLException {
        Path jsonFixture = Path.of("src/test/resources/fixtures/customers.json");
        String targetUrl = "jdbc:sqlite:" + tempDir.resolve("target.db");

        JsonDataSource source = new JsonDataSource(FileConnectionConfig.json(jsonFixture));
        JdbcDataSink sink = new JdbcDataSink(new JdbcConnectionConfig(targetUrl, null, null), "customers");

        TransferResult result = new TransferEngine().run(source, sink);

        assertThat(result.rowsWritten()).isEqualTo(3);

        try (Connection conn = DriverManager.getConnection(targetUrl)) {
            try (ResultSet rs = conn.createStatement().executeQuery("SELECT name, age FROM customers WHERE id = 3")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getString("name")).isEqualTo("Carol");
                assertThat(rs.getLong("age")).isEqualTo(40);
            }
        }
    }
}
