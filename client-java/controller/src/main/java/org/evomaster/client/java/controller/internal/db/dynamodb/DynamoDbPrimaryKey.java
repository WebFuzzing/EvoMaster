package org.evomaster.client.java.controller.internal.db.dynamodb;

import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbKeyScalarTypeDto;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable base-table primary-key attributes for insertion generation, ordered partition key then sort key
 * as it appears in the schema.
 */
public final class DynamoDbPrimaryKey {

    /**
     * Unmodifiable map from primary-key attribute name ({@link String}) to its DynamoDB scalar type
     * ({@link DynamoDbKeyScalarTypeDto}), ordered with the partition (hash) key first followed by the
     * optional sort (range) key.
     */
    private final Map<String, DynamoDbKeyScalarTypeDto> attributes;

    /**
     * Copies and validates the primary-key definition independently of its source map.
     *
     * @param attributes nonempty map from primary-key attribute name to scalar type, partition key before sort key
     * @throws NullPointerException when the mapping is null
     * @throws IllegalArgumentException when the mapping is empty, a name is missing, or a type is unsupported
     */
    public DynamoDbPrimaryKey(Map<String, DynamoDbKeyScalarTypeDto> attributes) {
        Objects.requireNonNull(attributes, "DynamoDB primary-key attributes cannot be null");
        Map<String, DynamoDbKeyScalarTypeDto> copy = new LinkedHashMap<>(attributes);
        if (copy.isEmpty()) {
            throw new IllegalArgumentException("DynamoDB primary key must contain at least one attribute");
        }
        copy.forEach((name, type) -> {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("DynamoDB primary-key attribute name cannot be null or empty");
            }
            if (type != DynamoDbKeyScalarTypeDto.STRING && type != DynamoDbKeyScalarTypeDto.NUMBER) {
                throw new IllegalArgumentException("Unsupported DynamoDB primary key type for " + name);
            }
        });
        this.attributes = Collections.unmodifiableMap(copy);
    }

    /**
     * Returns the attribute names and supported scalar types without exposing mutable state.
     *
     * @return immutable map from primary-key attribute name to scalar type, partition key before sort key
     */
    public Map<String, DynamoDbKeyScalarTypeDto> getAttributes() {
        return attributes;
    }
}