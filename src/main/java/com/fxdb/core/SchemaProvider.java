package com.fxdb.core;

import com.fxdb.core.model.TableSchema;

import java.util.List;

/** Discovers what tables/objects a source or target exposes, regardless of underlying technology. */
public interface SchemaProvider extends AutoCloseable {

    List<String> listTables();

    TableSchema describeTable(String tableName);

    @Override
    void close();
}
