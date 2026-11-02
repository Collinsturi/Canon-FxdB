package com.fxdb.transfer;

import com.fxdb.core.DataSink;
import com.fxdb.core.DataSource;
import com.fxdb.core.model.RowBatch;

import java.util.Iterator;

/**
 * Phase 1: direct, synchronous source-to-target transfer with no column remapping yet
 * (mapping support lands once MappingEngine exists in Phase 3; UI progress/cancel lands in Phase 4).
 */
public class TransferEngine {

    private static final int DEFAULT_BATCH_SIZE = 500;

    public TransferResult run(DataSource source, DataSink sink) {
        return run(source, sink, DEFAULT_BATCH_SIZE);
    }

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
}
