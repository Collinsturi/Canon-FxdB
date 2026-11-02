package com.fxdb.mapping;

import com.fxdb.core.model.DataType;
import com.fxdb.mapping.model.ColumnMapping;
import com.fxdb.mapping.model.ColumnTransform;
import com.fxdb.mapping.model.ConnectionDescriptor;
import com.fxdb.mapping.model.MigrationProfile;
import com.fxdb.mapping.model.TableMapping;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MappingProfileSerializerTest {

    @Test
    void roundTripsAProfileThroughJson(@TempDir Path tempDir) {
        ColumnMapping columnMapping = new ColumnMapping("customer_id", "cust_id", DataType.LONG,
                ColumnTransform.NONE, 0.95, true);
        TableMapping tableMapping = new TableMapping("customers", "clients", List.of(columnMapping), true);

        MigrationProfile profile = new MigrationProfile(
                ConnectionDescriptor.jdbc("jdbc:sqlite:source.db", null),
                ConnectionDescriptor.jdbc("jdbc:sqlite:target.db", null),
                List.of(tableMapping));

        Path profilePath = tempDir.resolve("profile.json");
        MappingProfileSerializer.save(profile, profilePath);
        MigrationProfile reloaded = MappingProfileSerializer.load(profilePath);

        assertThat(reloaded.getSource().getSettings()).containsEntry("jdbcUrl", "jdbc:sqlite:source.db");
        assertThat(reloaded.getTableMappings()).hasSize(1);

        TableMapping reloadedTable = reloaded.getTableMappings().get(0);
        assertThat(reloadedTable.getSourceTable()).isEqualTo("customers");
        assertThat(reloadedTable.getTargetTable()).isEqualTo("clients");

        ColumnMapping reloadedColumn = reloadedTable.getColumns().get(0);
        assertThat(reloadedColumn.getSourceColumn()).isEqualTo("customer_id");
        assertThat(reloadedColumn.getTargetColumn()).isEqualTo("cust_id");
        assertThat(reloadedColumn.getTargetType()).isEqualTo(DataType.LONG);
        assertThat(reloadedColumn.getMatchConfidence()).isEqualTo(0.95);
    }

    @Test
    void doesNotPersistAPasswordField() throws Exception {
        MigrationProfile profile = new MigrationProfile(
                ConnectionDescriptor.jdbc("jdbc:postgresql://localhost/db", "alice"),
                ConnectionDescriptor.jdbc("jdbc:postgresql://localhost/db2", "bob"),
                List.of());

        Path profilePath = java.nio.file.Files.createTempFile("profile", ".json");
        try {
            MappingProfileSerializer.save(profile, profilePath);
            String json = java.nio.file.Files.readString(profilePath);
            assertThat(json).doesNotContain("password");
        } finally {
            java.nio.file.Files.deleteIfExists(profilePath);
        }
    }
}
