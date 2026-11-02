package com.fxdb.core;

import com.fxdb.core.model.RowBatch;
import com.fxdb.core.model.TableSchema;

/** Write side of the database-agnostic transfer pipeline (JDBC table, CSV file, etc). */
public interface DataSink extends AutoCloseable {

    void open(TableSchema targetSchema, boolean createIfMissing);

    void writeBatch(RowBatch batch);

    void commit();

    @Override
    void close();
}
