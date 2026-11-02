package com.fxdb.file.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fxdb.core.exception.SchemaException;

import java.io.IOException;

/**
 * Reads a single flat JSON scalar value under the parser's current field. MVP scope is
 * flat array-of-objects only; a nested object or array fails fast rather than being
 * silently mishandled.
 */
final class JsonValueCodec {

    private JsonValueCodec() {
    }

    static Object readScalar(JsonParser parser, String fieldName) throws IOException {
        JsonToken token = parser.currentToken();
        return switch (token) {
            case VALUE_NULL -> null;
            case VALUE_NUMBER_INT -> parser.getLongValue();
            case VALUE_NUMBER_FLOAT -> parser.getDoubleValue();
            case VALUE_TRUE, VALUE_FALSE -> parser.getBooleanValue();
            case VALUE_STRING -> parser.getText();
            case START_OBJECT, START_ARRAY -> throw new SchemaException(
                    "Nested JSON object/array in field '" + fieldName + "' is not supported; "
                            + "only flat arrays of objects can be migrated in this version.");
            default -> throw new SchemaException("Unexpected JSON token " + token + " for field '" + fieldName + "'");
        };
    }
}
