package com.fxdb.transfer;

/**
 * What to do when a batch fails to write. Because JDBC writes happen via addBatch()/
 * executeBatch(), failure granularity is per-batch, not strictly per-row - SKIP_ROW skips
 * the whole offending batch, which is exactly one row when batchSize is 1.
 */
public enum ErrorPolicy {
    ABORT,
    SKIP_ROW,
    RETRY_THEN_SKIP
}
