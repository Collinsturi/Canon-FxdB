package com.fxdb.transfer;

import com.fxdb.core.DataSink;
import com.fxdb.core.DataSource;
import com.fxdb.core.model.RowBatch;
import com.fxdb.mapping.model.TableMapping;

import java.util.Iterator;

/**
 * Synchronous source-to-target transfer. UI progress/cancel and per-row error policy are
 * still to come in Phase 4; this is just the core read -> (optionally remap) -> write loop.
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
}
