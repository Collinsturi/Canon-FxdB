package com.fxdb.db.jdbc;

import com.fxdb.core.exception.FxDbException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DatabaseProvisionerTest {

    @Test
    void refusesToCreateADatabaseForAnUnsupportedVendor() {
        assertThatThrownBy(() -> DatabaseProvisioner.ensureDatabaseExists(
                DriverRegistry.ORACLE, "jdbc:oracle:thin:@//localhost:1521/XE", "new_db", "user", "pass"))
                .isInstanceOf(FxDbException.class)
                .hasMessageContaining("Oracle");
    }

    @Test
    void refusesForSqliteToo() {
        assertThatThrownBy(() -> DatabaseProvisioner.ensureDatabaseExists(
                DriverRegistry.SQLITE, "jdbc:sqlite:test.db", "new_db", null, null))
                .isInstanceOf(FxDbException.class)
                .hasMessageContaining("SQLite");
    }
}
