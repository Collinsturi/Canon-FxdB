package com.fxdb.transfer;

import com.fxdb.core.DataSink;
import com.fxdb.core.DataSource;
import com.fxdb.core.model.RowBatch;
import com.fxdb.mapping.model.TableMapping;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Synchronous source-to-target transfer, in two flavors: the plain run() overloads below are
 * the simple all-or-nothing path used by Phases 1-3's tests, while runWithProgress() adds the
 * instrumentation (progress callbacks, cancellation, per-batch error policy) the UI needs.
 */
public class TransferEngine {

    private static final int DEFAULT_BATCH_SIZE = 500;

    public TransferResult run(DataSource source, DataSink sink) {
        return run(source, sink, DEFAULT_BATCH_SIZE);
    }

    /** Direct 1:1 passthrough, used before a TableMapping exists (e.g. same-shape source/target). */
    public TransferResult run(DataSource source, DataSink sink, int batchSize) {
        source.open();
        sink.open(source.getSchema(), true);

        long rowsRead = 0;
        long rowsWritten = 0;
        try {
            Iterator<RowBatch> batches = source.readBatches(batchSize);
            while (batches.hasNext()) {
                RowBatch batch = batches.next();
                sink.writeBatch(batch);
                rowsRead += batch.size();
                rowsWritten += batch.size();
            }
            sink.commit();
        } finally {
            source.close();
            sink.close();
        }
        return new TransferResult(rowsRead, rowsWritten);
    }

    public TransferResult run(DataSource source, DataSink sink, TableMapping mapping) {
        return run(source, sink, mapping, DEFAULT_BATCH_SIZE);
    }

    /** Runs the transfer through a MappingEngine-produced (or hand-edited) TableMapping, renaming and transforming each row. */
    public TransferResult run(DataSource source, DataSink sink, TableMapping mapping, int batchSize) {
        source.open();
        sink.open(mapping.toTargetSchema(), true);

        long rowsRead = 0;
        long rowsWritten = 0;
        try {
            Iterator<RowBatch> batches = source.readBatches(batchSize);
            while (batches.hasNext()) {
                RowBatch batch = batches.next();
                rowsRead += batch.size();
                RowBatch converted = RowConverter.convert(batch, mapping);
                sink.writeBatch(converted);
                rowsWritten += converted.size();
            }
            sink.commit();
        } finally {
            source.close();
            sink.close();
        }
        return new TransferResult(rowsRead, rowsWritten);
    }

    public TableTransferResult runWithProgress(DataSource source, DataSink sink, TableMapping mapping,
                                                ErrorPolicy policy, ProgressListener listener, AtomicBoolean cancelFlag) {
        return runWithProgress(source, sink, mapping, policy, listener, cancelFlag, DEFAULT_BATCH_SIZE);
    }

    /**
     * Runs one table's transfer with full instrumentation: reports progress through
     * {@code listener}, checks {@code cancelFlag} between batches, and on a batch write
     * failure either aborts the whole table (ABORT), counts the batch as skipped and keeps
     * going (SKIP_ROW), or retries a few times first (RETRY_THEN_SKIP).
     */
    public TableTransferResult runWithProgress(DataSource source, DataSink sink, TableMapping mapping,
                                                ErrorPolicy policy, ProgressListener listener,
                                                AtomicBoolean cancelFlag, int batchSize) {
        String tableName = mapping.getTargetTable();
        List<String> errors = new ArrayList<>();
        long rowsRead = 0;
        long rowsWritten = 0;
        long rowsSkipped = 0;
        TableTransferResult.Status status = TableTransferResult.Status.SUCCESS;

        source.open();
        listener.onTableStart(tableName, source.estimateRowCount());
        sink.open(mapping.toTargetSchema(), true);

        try {
            Iterator<RowBatch> batches = source.readBatches(batchSize);
            batchLoop:
            while (batches.hasNext()) {
                if (cancelFlag != null && cancelFlag.get()) {
                    status = TableTransferResult.Status.CANCELLED;
                    break;
                }
                RowBatch batch = batches.next();
                rowsRead += batch.size();
                RowBatch converted = RowConverter.convert(batch, mapping);

                try {
                    writeWithRetry(sink, converted, policy);
                    rowsWritten += converted.size();
                } catch (RuntimeException e) {
                    String message = tableName + ": " + e.getMessage();
                    errors.add(message);
                    listener.onError(tableName, message);
                    if (policy == ErrorPolicy.ABORT) {
                        status = TableTransferResult.Status.FAILED;
                        break batchLoop;
                    }
                    rowsSkipped += converted.size();
                }
                listener.onBatchProgress(tableName, rowsRead);
            }
            if (status == TableTransferResult.Status.SUCCESS || status == TableTransferResult.Status.PARTIAL) {
                sink.commit();
                if (!errors.isEmpty()) {
                    status = TableTransferResult.Status.PARTIAL;
                }
            }
        } finally {
            source.close();
            sink.close();
        }

        TableTransferResult result = new TableTransferResult(tableName, rowsRead, rowsWritten, rowsSkipped, status, errors);
        listener.onTableComplete(result);
        return result;
    }

    private void writeWithRetry(DataSink sink, RowBatch batch, ErrorPolicy policy) {
        int attempts = policy == ErrorPolicy.RETRY_THEN_SKIP ? 3 : 1;
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < attempts; attempt++) {
            try {
                sink.writeBatch(batch);
                return;
            } catch (RuntimeException e) {
                lastFailure = e;
            }
        }
        throw lastFailure;
    }
}
