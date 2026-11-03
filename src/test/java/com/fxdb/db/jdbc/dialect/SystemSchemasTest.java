package com.fxdb.db.jdbc.dialect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SystemSchemasTest {

    @Test
    void sqlServerExcludesSysAndInformationSchema() {
        assertThat(new SqlServerDialect().systemSchemas())
                .containsExactlyInAnyOrder("sys", "information_schema");
    }

    @Test
    void mySqlExcludesItsBuiltInSchemas() {
        assertThat(new MySqlDialect().systemSchemas())
                .containsExactlyInAnyOrder("information_schema", "performance_schema", "mysql", "sys");
    }

    @Test
    void postgresExcludesPgCatalogAndInformationSchema() {
        assertThat(new PostgresDialect().systemSchemas())
                .containsExactlyInAnyOrder("pg_catalog", "information_schema");
    }

    @Test
    void dialectsWithoutSystemSchemasDefaultToEmpty() {
        assertThat(new SqliteDialect().systemSchemas()).isEmpty();
        assertThat(new OracleDialect().systemSchemas()).isEmpty();
    }
}
