package org.evomaster.client.java.controller.internal.db.dynamodb;

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbScalarTypeDto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable primary-key attributes supported by DynamoDB insertion generation, in schema order. */
final class DynamoDbPrimaryKey {

    private final Map<String, DynamoDbScalarTypeDto> attributes;

    /**
     * Copies and validates the primary-key definition independently of its source map.
     *
     * @param attributes nonempty attribute-name to scalar-type mapping in schema order
     * @throws NullPointerException when the mapping is null
     * @throws IllegalArgumentException when the mapping is empty, a name is missing, or a type is unsupported
     */
    DynamoDbPrimaryKey(Map<String, DynamoDbScalarTypeDto> attributes) {
        Objects.requireNonNull(attributes, "DynamoDB primary-key attributes cannot be null");
        Map<String, DynamoDbScalarTypeDto> copy = new LinkedHashMap<>(attributes);
        if (copy.isEmpty()) {
            throw new IllegalArgumentException("DynamoDB primary key must contain at least one attribute");
        }
        copy.forEach((name, type) -> {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("DynamoDB primary-key attribute name cannot be null or empty");
            }
            if (type != DynamoDbScalarTypeDto.STRING && type != DynamoDbScalarTypeDto.NUMBER) {
                throw new IllegalArgumentException("Unsupported DynamoDB primary key type for " + name);
            }
        });
        this.attributes = Collections.unmodifiableMap(copy);
    }

    /**
     * Returns the attribute names and supported scalar types without exposing mutable state.
     *
     * @return immutable attribute mapping in schema order
     */
    Map<String, DynamoDbScalarTypeDto> getAttributes() {
        return attributes;
    }
}
