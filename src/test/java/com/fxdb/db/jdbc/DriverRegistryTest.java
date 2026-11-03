package com.fxdb.db.jdbc;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DriverRegistryTest {

    @Test
    void buildsPostgresUrlFromHostPortDatabase() {
        String url = DriverRegistry.POSTGRES.buildUrl("localhost", 5432, "widgets");
        assertThat(url).isEqualTo("jdbc:postgresql://localhost:5432/widgets");
        assertThat(DriverRegistry.fromJdbcUrl(url)).isEqualTo(DriverRegistry.POSTGRES);
    }

    @Test
    void buildsMysqlUrlFromHostPortDatabase() {
        String url = DriverRegistry.MYSQL.buildUrl("localhost", 3306, "widgets");
        assertThat(url).isEqualTo("jdbc:mysql://localhost:3306/widgets");
        assertThat(DriverRegistry.fromJdbcUrl(url)).isEqualTo(DriverRegistry.MYSQL);
    }

    @Test
    void buildsSqlServerUrlWithDatabaseNameParameter() {
        String url = DriverRegistry.SQLSERVER.buildUrl("localhost", 1433, "widgets");
        assertThat(url).isEqualTo("jdbc:sqlserver://localhost:1433;databaseName=widgets");
        assertThat(DriverRegistry.fromJdbcUrl(url)).isEqualTo(DriverRegistry.SQLSERVER);
    }

    @Test
    void buildsOracleUrlUsingSlashSlashForm() {
        String url = DriverRegistry.ORACLE.buildUrl("localhost", 1521, "widgets");
        assertThat(url).isEqualTo("jdbc:oracle:thin:@//localhost:1521/widgets");
        assertThat(DriverRegistry.fromJdbcUrl(url)).isEqualTo(DriverRegistry.ORACLE);
    }

    @Test
    void buildsSqliteUrlFromPathAlone() {
        String url = DriverRegistry.buildSqliteUrl("C:/data/widgets.sqlite");
        assertThat(url).isEqualTo("jdbc:sqlite:C:/data/widgets.sqlite");
        assertThat(DriverRegistry.fromJdbcUrl(url)).isEqualTo(DriverRegistry.SQLITE);
    }

    @Test
    void sqliteRejectsHostPortBuilder() {
        assertThatThrownBy(() -> DriverRegistry.SQLITE.buildUrl("localhost", 1, "widgets"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void defaultPortsMatchEachProvider() {
        assertThat(DriverRegistry.POSTGRES.defaultPort()).isEqualTo(5432);
        assertThat(DriverRegistry.MYSQL.defaultPort()).isEqualTo(3306);
        assertThat(DriverRegistry.SQLSERVER.defaultPort()).isEqualTo(1433);
        assertThat(DriverRegistry.ORACLE.defaultPort()).isEqualTo(1521);
        assertThat(DriverRegistry.SQLITE.isFileBased()).isTrue();
    }
}
