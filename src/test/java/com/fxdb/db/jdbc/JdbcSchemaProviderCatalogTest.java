package com.fxdb.db.jdbc;

import com.fxdb.core.model.TableSchema;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression coverage for passing the connection's own catalog to every DatabaseMetaData
 * lookup (see JdbcSchemaProvider.catalog()) instead of null. H2 only ever exposes a single
 * catalog per connection, so this can't reproduce the cross-catalog leak itself (that needs a
 * real multi-database SQL Server/MySQL instance) - it just proves introspection still finds
 * the right tables/columns/keys once the catalog argument is no longer null.
 */
class JdbcSchemaProviderCatalogTest {

    @Test
    void listTablesAndDescribeTableStillWorkWithExplicitCatalog(@TempDir Path tempDir) throws Exception {
        String jdbcUrl = "jdbc:h2:" + tempDir.resolve("catalogtest");

        try (var connection = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TABLE customers (id INT PRIMARY KEY, name VARCHAR(100))");
                statement.execute("CREATE TABLE orders (id INT PRIMARY KEY, customer_id INT, "
                        + "FOREIGN KEY (customer_id) REFERENCES customers(id))");
            }
        }

        try (var provider = new JdbcSchemaProvider(new JdbcConnectionConfig(jdbcUrl, "sa", ""))) {
            // H2 always surfaces its own INFORMATION_SCHEMA views alongside real tables regardless
            // of catalog scoping (that's H2-specific noise, not what this test is about) - just
            // assert the tables we created are found, not that nothing else is.
            assertThat(provider.listTables()).contains("CUSTOMERS", "ORDERS");

            TableSchema orders = provider.describeTable("ORDERS");
            assertThat(orders.columns()).extracting("name").containsExactlyInAnyOrder("ID", "CUSTOMER_ID");
            assertThat(orders.primaryKeyColumns()).containsExactly("ID");
            assertThat(orders.foreignKeys()).hasSize(1);
            assertThat(orders.foreignKeys().get(0).referencedTable()).isEqualTo("CUSTOMERS");
        }
    }
}
