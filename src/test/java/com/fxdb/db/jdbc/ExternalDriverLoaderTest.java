package com.fxdb.db.jdbc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Stands in for the real use case - dropping Oracle's ojdbc jar, which fxDb doesn't bundle,
 * into a "drivers" folder - using H2's jar instead, since it's already on the test
 * classpath and easy to locate. The point being proven is that ExternalDriverLoader can pick
 * up a driver jar from an arbitrary directory at runtime and make DriverManager actually use
 * it through DriverShim, not that H2 itself is somehow unavailable otherwise.
 */
class ExternalDriverLoaderTest {

    @Test
    void loadsAJarFromTheDriversFolderAndMakesItUsable(@TempDir Path tempDir) throws Exception {
        Path driversDir = tempDir.resolve("drivers");
        Files.createDirectory(driversDir);
        Files.copy(locateH2Jar(), driversDir.resolve("h2-standin.jar"));

        ExternalDriverLoader.loadDriversFrom(driversDir);

        String url = "jdbc:h2:mem:externalDriverLoaderTest;DB_CLOSE_DELAY=-1";
        boolean shimRegistered = DriverManager.drivers()
                .anyMatch(d -> d instanceof DriverShim && isUsableFor(d, url));
        assertThat(shimRegistered).isTrue();

        try (Connection conn = DriverManager.getConnection(url);
             Statement statement = conn.createStatement()) {
            statement.execute("CREATE TABLE t (id INT)");
            statement.execute("INSERT INTO t VALUES (42)");
            try (ResultSet rs = statement.executeQuery("SELECT id FROM t")) {
                assertThat(rs.next()).isTrue();
                assertThat(rs.getInt(1)).isEqualTo(42);
            }
        }
    }

    @Test
    void aMissingDriversDirectoryIsANoOp(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("does-not-exist");
        ExternalDriverLoader.loadDriversFrom(missing); // must not throw
    }

    private boolean isUsableFor(java.sql.Driver driver, String url) {
        try {
            return driver.acceptsURL(url);
        } catch (Exception e) {
            return false;
        }
    }

    private Path locateH2Jar() throws URISyntaxException {
        File location = new File(org.h2.Driver.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        return location.toPath();
    }
}
