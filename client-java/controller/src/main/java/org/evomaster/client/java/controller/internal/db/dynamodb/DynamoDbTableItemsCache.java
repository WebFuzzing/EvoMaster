package org.evomaster.client.java.controller.internal.db.dynamodb;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Caches normalized DynamoDB table items loaded during one command evaluation batch.
 */
final class DynamoDbTableItemsCache {

    private final DynamoDbTableDataAccessor tableDataAccessor;
    private final Object dynamoDbClient;
    private final Map<String, List<Map<String, Object>>> itemsByTable = new HashMap<>();

    /**
     * Creates a cache backed by the given table accessor and DynamoDB client.
     *
     * @param tableDataAccessor accessor used to scan tables on cache misses
     * @param dynamoDbClient SDK v2 synchronous or asynchronous DynamoDB client
     */
    DynamoDbTableItemsCache(DynamoDbTableDataAccessor tableDataAccessor, Object dynamoDbClient) {
        this.tableDataAccessor = Objects.requireNonNull(tableDataAccessor,
                "DynamoDB table data accessor cannot be null");
        this.dynamoDbClient = dynamoDbClient;
    }

    /**
     * Returns the cached items for a table, scanning it on the first request in this batch.
     *
     * @param tableName table to read
     * @return immutable normalized items for the table
     */
    List<Map<String, Object>> getItems(String tableName) {
        String validTableName = validateTableName(tableName);
        List<Map<String, Object>> items = itemsByTable.get(validTableName);
        if (items == null) {
            items = validateItems(tableDataAccessor.getItems(dynamoDbClient, validTableName));
            itemsByTable.put(validTableName, items);
        }
        return items;
    }

    /**
     * Checks that a table name can be used as a stable cache key.
     *
     * @param tableName candidate table name
     * @return validated table name
     */
    private String validateTableName(String tableName) {
        Objects.requireNonNull(tableName, "DynamoDB table name cannot be null");
        if (tableName.trim().isEmpty()) {
            throw new IllegalArgumentException("DynamoDB table name cannot be blank");
        }
        return tableName;
    }

    /**
     * Copies scanned items into an immutable structure and checks the cache invariants.
     *
     * @param items scanned table items
     * @return immutable table items
     */
    private List<Map<String, Object>> validateItems(List<Map<String, Object>> items) {
        Objects.requireNonNull(items, "DynamoDB table items cannot be null");
        List<Map<String, Object>> copy = new ArrayList<>(items.size());
        for (Map<String, Object> item : items) {
            Objects.requireNonNull(item, "DynamoDB table item cannot be null");
            for (String attributeName : item.keySet()) {
                Objects.requireNonNull(attributeName, "DynamoDB attribute name cannot be null");
            }
            copy.add(Collections.unmodifiableMap(new LinkedHashMap<>(item)));
        }
        return Collections.unmodifiableList(copy);
    }
}
