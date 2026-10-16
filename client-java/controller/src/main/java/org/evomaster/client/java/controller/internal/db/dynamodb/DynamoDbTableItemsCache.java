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
 * <p>
 * The cache maintains an unmodifiable list of item maps for each table accessed in the batch.
 * Note that while the outer list and the per-item attribute maps are unmodifiable, the underlying
 * {@link Object} attribute values (for instance, nested collections, maps, or byte arrays) are not
 * guaranteed to be deeply immutable.
 */
final class DynamoDbTableItemsCache {

    private final DynamoDbTableDataAccessor tableDataAccessor;
    private final Object dynamoDbClient;

    /**
     * Map from DynamoDB table name ({@link String}) to the list of items scanned from that table.
     * Each item is represented as a {@link Map} where the key is the attribute name ({@link String})
     * and the value is the attribute value ({@link Object}).
     */
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
     * @return unmodifiable list of normalized items for the table, where each item is represented
     *         as an unmodifiable map of attribute name ({@link String}) to attribute value ({@link Object}).
     *         Note that the individual attribute value {@link Object}s are not guaranteed to be
     *         deeply immutable.
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
     * Checks that a table name can be used as a stable cache key and returns its trimmed representation.
     *
     * @param tableName candidate table name
     * @return validated and trimmed table name
     */
    private String validateTableName(String tableName) {
        Objects.requireNonNull(tableName, "DynamoDB table name cannot be null");
        String trimmed = tableName.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("DynamoDB table name cannot be blank");
        }
        return trimmed;
    }

    /**
     * Copies scanned items into unmodifiable structures and checks the cache invariants.
     *
     * @param items scanned table items, where each item is a {@link Map} of attribute name ({@link String})
     *              to attribute value ({@link Object})
     * @return unmodifiable list of unmodifiable item maps. Note that individual attribute value
     *         {@link Object} instances within each map are not guaranteed to be deeply immutable.
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
