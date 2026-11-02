package com.fxdb.transfer;

import java.util.List;

public record TableTransferResult(
        String tableName,
        long rowsRead,
        long rowsWritten,
        long rowsSkipped,
        Status status,
        List<String> errors
) {
    public enum Status {
        SUCCESS,
        PARTIAL,
        FAILED,
        CANCELLED
    }
}
