package com.fxdb.mapping;

import com.fxdb.core.model.Column;
import com.fxdb.core.model.DataType;
import com.fxdb.core.model.TableSchema;
import com.fxdb.mapping.model.ColumnMapping;
import com.fxdb.mapping.model.TableMapping;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MappingEngineTest {

    @Test
    void newTargetTableMapsEveryColumnOneToOneWithFullConfidence() {
        TableSchema source = new TableSchema(null, null, "customers", List.of(
                new Column("id", DataType.LONG, false),
                new Column("name", DataType.STRING, false)
        ), List.of(), List.of());

        TableMapping mapping = MappingEngine.autoMatch(source, "customers", null);

        assertThat(mapping.getColumns()).hasSize(2);
        assertThat(mapping.getColumns()).allMatch(c -> c.getMatchConfidence() == 1.0 && c.isIncluded());
        assertThat(mapping.getColumns().get(0).getTargetColumn()).isEqualTo("id");
        assertThat(mapping.getColumns().get(1).getTargetColumn()).isEqualTo("name");
    }

    @Test
    void existingTargetMatchesColumnsByNameAndType() {
        TableSchema source = new TableSchema(null, null, "customers", List.of(
                new Column("customer_id", DataType.LONG, false),
                new Column("full_name", DataType.STRING, false),
                new Column("signup_date", DataType.DATE, true)
        ), List.of(), List.of());

        TableSchema target = new TableSchema(null, null, "clients", List.of(
                new Column("cust_id", DataType.LONG, false),
                new Column("name", DataType.STRING, false),
                new Column("signup_date", DataType.TIMESTAMP, true)
        ), List.of(), List.of());

        TableMapping mapping = MappingEngine.autoMatch(source, "clients", target);

        ColumnMapping idMapping = findBySource(mapping, "customer_id");
        assertThat(idMapping.getTargetColumn()).isEqualTo("cust_id");
        assertThat(idMapping.isIncluded()).isTrue();

        ColumnMapping dateMapping = findBySource(mapping, "signup_date");
        assertThat(dateMapping.getTargetColumn()).isEqualTo("signup_date");
    }

    @Test
    void sourceColumnWithNoRemainingTargetIsKeptButLeftUnmapped() {
        // Two source columns compete for a single target column; the clear winner takes it,
        // and the loser still shows up in the result (for manual review) rather than vanishing.
        TableSchema source = new TableSchema(null, null, "customers", List.of(
                new Column("customer_id", DataType.LONG, false),
                new Column("fax_number", DataType.STRING, true)
        ), List.of(), List.of());

        TableSchema target = new TableSchema(null, null, "clients", List.of(
                new Column("cust_id", DataType.LONG, false)
        ), List.of(), List.of());

        TableMapping mapping = MappingEngine.autoMatch(source, "clients", target);

        assertThat(mapping.getColumns()).hasSize(2);

        ColumnMapping idMapping = findBySource(mapping, "customer_id");
        assertThat(idMapping.getTargetColumn()).isEqualTo("cust_id");
        assertThat(idMapping.isIncluded()).isTrue();

        ColumnMapping faxMapping = findBySource(mapping, "fax_number");
        assertThat(faxMapping.getTargetColumn()).isNull();
        assertThat(faxMapping.isIncluded()).isFalse();
    }

    private ColumnMapping findBySource(TableMapping mapping, String sourceColumn) {
        return mapping.getColumns().stream()
                .filter(c -> c.getSourceColumn().equals(sourceColumn))
                .findFirst()
                .orElseThrow();
    }
}
