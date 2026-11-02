package com.fxdb.core;

import com.fxdb.core.model.RowBatch;
import com.fxdb.core.model.TableSchema;

import java.util.Iterator;

/**
 * Read side of the database-agnostic transfer pipeline. A JDBC table and a CSV file both
 * implement this the same way, so {@code TransferEngine} never needs to know which.
 */
public interface DataSource extends AutoCloseable {

    TableSchema getSchema();

    void open();

    Iterator<RowBatch> readBatches(int batchSize);

    /** Best-effort row count for progress reporting; -1 if unknown/unavailable. */
    long estimateRowCount();

    @Override
    void close();
}
