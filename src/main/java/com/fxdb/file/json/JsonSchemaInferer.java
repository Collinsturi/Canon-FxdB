package com.fxdb.file.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fxdb.core.exception.SchemaException;
import com.fxdb.core.model.Column;
import com.fxdb.core.model.DataType;
import com.fxdb.core.model.TableSchema;
import com.fxdb.file.FileConnectionConfig;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Infers a TableSchema from a sample of objects in a JSON file shaped as a flat array of
 * objects, streaming token-by-token so the whole array never has to be loaded into memory.
 */
public final class JsonSchemaInferer {

    private static final int SAMPLE_SIZE = 100;
    private static final JsonFactory FACTORY = new JsonFactory();

    private JsonSchemaInferer() {
    }

    public static TableSchema infer(FileConnectionConfig config) {
        Map<String, DataType> columnTypes = new LinkedHashMap<>();
        Map<String, Boolean> nullable = new LinkedHashMap<>();

        try (JsonParser parser = FACTORY.createParser(config.path().toFile())) {
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                throw new SchemaException("Expected a top-level JSON array in " + config.path());
            }

            int sampled = 0;
            while (parser.nextToken() == JsonToken.START_OBJECT && sampled < SAMPLE_SIZE) {
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    String fieldName = parser.currentName();
                    parser.nextToken();
                    JsonToken valueToken = parser.currentToken();

                    if (valueToken == JsonToken.VALUE_NULL) {
                        nullable.put(fieldName, true);
                        columnTypes.putIfAbsent(fieldName, null);
                        continue;
                    }
                    if (valueToken == JsonToken.START_OBJECT || valueToken == JsonToken.START_ARRAY) {
                        throw new SchemaException(
                                "Nested JSON object/array in field '" + fieldName + "' is not supported; "
                                        + "only flat arrays of objects can be migrated in this version.");
                    }

                    DataType valueType = tokenToDataType(valueToken);
                    columnTypes.merge(fieldName, valueType, JsonSchemaInferer::widen);
                    nullable.putIfAbsent(fieldName, false);
                }
                sampled++;
            }
        } catch (IOException e) {
            throw new SchemaException("Failed to infer schema from JSON file " + config.path(), e);
        }

        List<Column> columns = columnTypes.entrySet().stream()
                .map(e -> new Column(e.getKey(),
                        e.getValue() == null ? DataType.STRING : e.getValue(),
                        nullable.getOrDefault(e.getKey(), false)))
                .toList();

        return new TableSchema(null, null, config.fileBaseName(), columns, List.of(), List.of());
    }

    private static DataType tokenToDataType(JsonToken token) {
        return switch (token) {
            case VALUE_NUMBER_INT -> DataType.LONG;
            case VALUE_NUMBER_FLOAT -> DataType.DOUBLE;
            case VALUE_TRUE, VALUE_FALSE -> DataType.BOOLEAN;
            case VALUE_STRING -> DataType.STRING;
            default -> DataType.STRING;
        };
    }

    private static DataType widen(DataType a, DataType b) {
        if (a == null) return b;
        if (b == null) return a;
        if (a == b) return a;
        boolean aNumeric = a == DataType.LONG || a == DataType.DOUBLE;
        boolean bNumeric = b == DataType.LONG || b == DataType.DOUBLE;
        if (aNumeric && bNumeric) return DataType.DOUBLE;
        return DataType.STRING;
    }
}
