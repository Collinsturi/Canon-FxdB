package com.fxdb.db.jdbc;

import com.fxdb.core.exception.SchemaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JdbcSchemaProviderConnectionErrorTest {

    @Test
    void wrongPasswordSurfacesTheDriversRealMessage(@TempDir Path tempDir) {
        String jdbcUrl = "jdbc:h2:" + tempDir.resolve("errtest");

        // First connection creates the database and sets the "sa" user's password.
        try (var provider = new JdbcSchemaProvider(new JdbcConnectionConfig(jdbcUrl, "sa", "correct-password"))) {
            provider.listTables();
        }

        try (var provider = new JdbcSchemaProvider(new JdbcConnectionConfig(jdbcUrl, "sa", "wrong-password"))) {
            assertThatThrownBy(provider::listTables)
                    .isInstanceOf(SchemaException.class)
                    .satisfies(e -> {
                        String message = e.getMessage();
                        assertThat(message).contains("Failed to connect to " + jdbcUrl);
                        // The driver's own text, not just the generic prefix, must be present.
                        assertThat(message).containsIgnoringCase("wrong user name or password");
                        assertThat(message).contains("[SQLState:");
                    });
        }
    }
}
