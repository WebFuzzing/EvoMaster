package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.List;

/**
 * Builds stable identifiers for inferred DynamoDB insertions.
 */
public final class DynamoDbInsertionKeyBuilder {

    /**
     * Prevents instantiation of this utility class.
     */
    private DynamoDbInsertionKeyBuilder() {
    }

    /**
     * Builds the insertion key from a table and its ordered typed attributes.
     *
     * @param tableName target DynamoDB table
     * @param attributes ordered attributes in the insertion
     * @return stable key in the {@code table|name:type=value} format
     */
    public static String fromAttributes(String tableName, List<DynamoDbAttributeValueDto> attributes) {
        StringBuilder insertionKey = new StringBuilder(tableName);
        for (DynamoDbAttributeValueDto attribute : attributes) {
            insertionKey.append('|').append(attribute.attributeName)
                    .append(':').append(attribute.type).append('=');
            appendValue(insertionKey, attribute);
        }
        return insertionKey.toString();
    }

    /**
     * Appends a deterministic, length-delimited representation of nested payloads.
     *
     * @param key target string builder
     * @param attribute attribute whose value is being formatted
     */
    private static void appendValue(StringBuilder key, DynamoDbAttributeValueDto attribute) {
        if (attribute.type == DynamoDbValueTypeDto.MAP) {
            key.append('{');
            new java.util.TreeMap<>(attribute.mapValue).forEach((name, child) -> {
                key.append(name.length()).append(':').append(name).append(':').append(child.type).append('=');
                appendNestedValue(key, child);
            });
            key.append('}');
        } else if (attribute.type == DynamoDbValueTypeDto.LIST) {
            key.append('[');
            for (DynamoDbAttributeValueDto child : attribute.listValue) {
                key.append(child.type).append('=');
                appendNestedValue(key, child);
            }
            key.append(']');
        } else {
            key.append(attribute.value);
        }
    }

    /**
     * Delimits a child payload so embedded punctuation cannot hide a structural change.
     *
     * @param key target string builder
     * @param child child attribute whose value is being delimited
     */
    private static void appendNestedValue(StringBuilder key, DynamoDbAttributeValueDto child) {
        StringBuilder value = new StringBuilder();
        appendValue(value, child);
        key.append(value.length()).append(':').append(value);
    }
}
