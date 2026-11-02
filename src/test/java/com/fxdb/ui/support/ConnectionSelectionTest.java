package com.fxdb.ui.support;

import com.fxdb.core.DataSink;
import com.fxdb.core.DataSource;
import com.fxdb.core.SchemaProvider;
import com.fxdb.db.jdbc.JdbcConnectionConfig;
import com.fxdb.file.FileConnectionConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConnectionSelectionTest {

    @Test
    void jdbcSelectionListsTablesAndBuildsWorkingSourceAndSink(@TempDir Path tempDir) throws SQLException {
        String url = "jdbc:sqlite:" + tempDir.resolve("db.sqlite");
        try (Connection conn = DriverManager.getConnection(url);
             Statement statement = conn.createStatement()) {
            statement.execute("CREATE TABLE widgets (id INTEGER PRIMARY KEY, label TEXT)");
            statement.execute("INSERT INTO widgets (id, label) VALUES (1, 'bolt')");
        }

        ConnectionSelection selection = new ConnectionSelection.Jdbc(new JdbcConnectionConfig(url, null, null));

        try (SchemaProvider provider = selection.createSchemaProvider()) {
            assertThat(provider.listTables()).contains("widgets");
        }

        DataSource source = selection.createDataSource("widgets");
        assertThat(source).isNotNull();
        source.open();
        assertThat(source.getSchema().tableName()).isEqualTo("widgets");
        source.close();

        assertThat(selection.toDescriptor().getSettings()).containsEntry("jdbcUrl", url);
    }

    @Test
    void fileSelectionListsCsvAsASingleTableNamedAfterTheFile(@TempDir Path tempDir) throws Exception {
        Path csv = tempDir.resolve("widgets.csv");
        java.nio.file.Files.writeString(csv, "id,label\n1,bolt\n");

        ConnectionSelection selection = new ConnectionSelection.File(FileConnectionConfig.csv(csv));

        try (SchemaProvider provider = selection.createSchemaProvider()) {
            List<String> tables = provider.listTables();
            assertThat(tables).containsExactly("widgets");
        }

        DataSink sink = selection.createDataSink("widgets");
        assertThat(sink).isNotNull();
    }
}
