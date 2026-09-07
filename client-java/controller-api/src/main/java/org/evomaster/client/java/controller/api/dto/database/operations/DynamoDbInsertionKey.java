package org.evomaster.client.java.controller.api.dto.database.operations;

import java.util.List;

/**
 * Builds stable identifiers for inferred DynamoDB insertions.
 */
public final class DynamoDbInsertionKey {

    /**
     * Prevents instantiation of this utility class.
     */
    private DynamoDbInsertionKey() {
    }

    /**
     * Builds the insertion key from a table and its ordered scalar attributes.
     *
     * @param tableName target DynamoDB table
     * @param attributes ordered attributes in the insertion
     * @return stable key in the {@code table|name:type=value} format
     */
    public static String fromAttributes(String tableName, List<DynamoDbAttributeValueDto> attributes) {
        StringBuilder insertionKey = new StringBuilder(tableName);
        for (DynamoDbAttributeValueDto attribute : attributes) {
            insertionKey.append('|').append(attribute.attributeName)
                    .append(':').append(attribute.type).append('=').append(attribute.value);
        }
        return insertionKey.toString();
    }
}
