package com.fxdb.transfer;

import java.util.List;

/** Aggregate result of running every table in a MigrationProfile through the engine. */
public record MigrationRunResult(List<TableTransferResult> tableResults) {

    public boolean anyFailed() {
        return tableResults.stream().anyMatch(r -> r.status() == TableTransferResult.Status.FAILED);
    }

    public long totalRowsWritten() {
        return tableResults.stream().mapToLong(TableTransferResult::rowsWritten).sum();
    }
}
