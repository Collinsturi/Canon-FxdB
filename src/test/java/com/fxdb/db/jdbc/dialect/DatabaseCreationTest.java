package com.fxdb.db.jdbc.dialect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseCreationTest {

    @Test
    void postgresConnectsToItsMaintenanceDatabase() {
        PostgresDialect dialect = new PostgresDialect();
        assertThat(dialect.supportsDatabaseCreation()).isTrue();
        assertThat(dialect.adminDatabase()).isEqualTo("postgres");
        assertThat(dialect.createDatabaseDdl("new_db")).isEqualTo("CREATE DATABASE \"new_db\"");
    }

    @Test
    void mySqlConnectsWithNoDatabaseSelected() {
        MySqlDialect dialect = new MySqlDialect();
        assertThat(dialect.supportsDatabaseCreation()).isTrue();
        assertThat(dialect.adminDatabase()).isEmpty();
        assertThat(dialect.createDatabaseDdl("new_db")).isEqualTo("CREATE DATABASE `new_db`");
    }

    @Test
    void sqlServerConnectsToMaster() {
        SqlServerDialect dialect = new SqlServerDialect();
        assertThat(dialect.supportsDatabaseCreation()).isTrue();
        assertThat(dialect.adminDatabase()).isEqualTo("master");
        assertThat(dialect.createDatabaseDdl("new_db")).isEqualTo("CREATE DATABASE \"new_db\"");
    }

    @Test
    void sqliteAndOracleDontSupportDatabaseCreation() {
        assertThat(new SqliteDialect().supportsDatabaseCreation()).isFalse();
        assertThat(new OracleDialect().supportsDatabaseCreation()).isFalse();

        assertThatThrownBy(() -> new SqliteDialect().adminDatabase())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new OracleDialect().createDatabaseDdl("x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
