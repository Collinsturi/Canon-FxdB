package com.fxdb.transfer;

import com.fxdb.core.DataSink;
import com.fxdb.core.DataSource;
import com.fxdb.mapping.model.TableMapping;

/** One table's worth of work for TransferTask: where to read, where to write, and how to map between them. */
public record TableJob(DataSource source, DataSink sink, TableMapping mapping) {
}
