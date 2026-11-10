package org.evomaster.client.java.instrumentation.dynamodb;


import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Serializable query data copied from an intercepted DynamoDB SDK request.
 * Includes reads, writes, and batch requests so their required fields can cross JVM boundaries.
 */
public final class DynamoDbQuery implements Serializable {

    private final String tableName;
    /** Maps each key attribute name to its typed value. */
    private final Map<String, DynamoDbQueryValue> key;
    /** Maps each table name to its batch-get key collection. */
    private final Map<String, BatchGetKeys> batchRequestItems;
    private final String keyConditionExpression;
    private final String filterExpression;
    /** Maps each expression name placeholder to its attribute name. */
    private final Map<String, String> expressionAttributeNames;
    /** Maps each expression value placeholder to its typed value. */
    private final Map<String, DynamoDbQueryValue> expressionAttributeValues;

    private DynamoDbQuery(Object request) {
        tableName = readString(request, "tableName");
        key = snapshotAttributeMap(read(request, "key"));
        batchRequestItems = snapshotRequestItems(read(request, "requestItems"));
        keyConditionExpression = readString(request, "keyConditionExpression");
        String readFilterExpression = readString(request, "filterExpression");
        //reuse the same field for conditionExpression which only exists in Write queries.
        filterExpression = readFilterExpression == null
                ? readString(request, "conditionExpression") : readFilterExpression;
        expressionAttributeNames = snapshotNameMap(read(request, "expressionAttributeNames"));
        expressionAttributeValues = snapshotAttributeMap(read(request, "expressionAttributeValues"));
    }

    /**
     * Copies the fields required by the supported request parsers into serializable query data.
     *
     * @param request original SDK request
     * @return serializable query data, or {@code null} when the request is null
     */
    public static DynamoDbQuery from(Object request) {
        return request == null ? null : new DynamoDbQuery(request);
    }

    /**
     * Returns the request table name.
     *
     * @return table name, or {@code null}
     */
    public String tableName() {
        return tableName;
    }

    /**
     * Returns the typed item key.
     *
     * @return attribute name to typed key value map, or {@code null}
     */
    public Map<String, DynamoDbQueryValue> key() {
        return key;
    }

    /**
     * Returns batch request items by table.
     *
     * @return table name to batch-get keys map, or {@code null}
     */
    public Map<String, BatchGetKeys> batchRequestItems() {
        return batchRequestItems;
    }

    /**
     * Returns the key-condition expression.
     *
     * @return expression, or {@code null}
     */
    public String keyConditionExpression() {
        return keyConditionExpression;
    }

    /**
     * Returns the read filter or write condition expression.
     *
     * @return expression, or {@code null}
     */
    public String filterExpression() {
        return filterExpression;
    }

    /**
     * Returns the expression attribute names.
     *
     * @return expression name placeholder to attribute name map
     */
    public Map<String, String> expressionAttributeNames() {
        return expressionAttributeNames;
    }

    /**
     * Returns the expression attribute values.
     *
     * @return expression value placeholder to typed value map
     */
    public Map<String, DynamoDbQueryValue> expressionAttributeValues() {
        return expressionAttributeValues;
    }

    /**
     * Returns the predicate shape used to group heuristic entries for one table.
     * Attribute values and key values are excluded, so equivalent predicates share an identifier.
     *
     * @param batchGetTableName table whose batch-get key fields should be included
     * @return predicate expressions, name aliases, and sorted key field names
     */
    public String predicateSignatureForTable(String batchGetTableName) {
        Set<String> keyFields = new TreeSet<>();
        if (key != null) {
            keyFields.addAll(key.keySet());
        }
        if (batchRequestItems != null) {
            BatchGetKeys tableItems = batchRequestItems.get(batchGetTableName);
            if (tableItems != null && tableItems.keys() != null) {
                for (Map<String, DynamoDbQueryValue> batchKey : tableItems.keys()) {
                    keyFields.addAll(batchKey.keySet());
                }
            }
        }

        return "keyFields=" + keyFields
                + ";keyConditionExpression=" + keyConditionExpression
                + ";filterExpression=" + filterExpression
                + ";expressionAttributeNames=" + (expressionAttributeNames == null
                ? null : new TreeMap<>(expressionAttributeNames));
    }

    /** Copies a map from table names to their batch-get key collections. */
    private static Map<String, BatchGetKeys> snapshotRequestItems(Object requestItems) {
        if (!(requestItems instanceof Map<?, ?>)) {
            return null;
        }

        Map<String, BatchGetKeys> result = new LinkedHashMap<>();
        ((Map<?, ?>) requestItems).forEach((table, keysAndAttributes) -> {
            if (table != null) {
                result.put(String.valueOf(table), new BatchGetKeys(keysAndAttributes));
            }
        });
        return Collections.unmodifiableMap(result);
    }

    /** Copies a map from expression name placeholders to attribute names. */
    private static Map<String, String> snapshotNameMap(Object source) {
        if (!(source instanceof Map<?, ?>)) {
            return null;
        }

        Map<String, String> result = new LinkedHashMap<>();
        ((Map<?, ?>) source).forEach((key, value) -> {
            if (key != null && value != null) {
                result.put(String.valueOf(key), String.valueOf(value));
            }
        });
        return Collections.unmodifiableMap(result);
    }

    private static DynamoDbQueryValue snapshotAttributeValue(Object attributeValue) {
        Object nul = read(attributeValue, "nul");
        if (Boolean.TRUE.equals(nul)) {
            return DynamoDbQueryValue.nullValue();
        }

        Object stringValue = read(attributeValue, "s");
        if (stringValue instanceof String) {
            return DynamoDbQueryValue.string((String) stringValue);
        }

        Object numberValue = read(attributeValue, "n");
        if (numberValue instanceof String && !((String) numberValue).isEmpty()) {
            return DynamoDbQueryValue.number((String) numberValue);
        }

        Object booleanValue = read(attributeValue, "bool");
        if (booleanValue instanceof Boolean) {
            return DynamoDbQueryValue.bool((Boolean) booleanValue);
        }

        Object mapValue = read(attributeValue, "m");
        if (isPresent(attributeValue, "hasM", mapValue) && mapValue instanceof Map<?, ?>) {
            return DynamoDbQueryValue.map(snapshotAttributeMap(mapValue));
        }

        Object listValue = read(attributeValue, "l");
        if (isPresent(attributeValue, "hasL", listValue) && listValue instanceof Collection<?>) {
            return DynamoDbQueryValue.list(snapshotAttributeValues((Collection<?>) listValue));
        }

        Object stringSet = read(attributeValue, "ss");
        if (isPresent(attributeValue, "hasSs", stringSet) && stringSet instanceof Collection<?>) {
            return DynamoDbQueryValue.stringSet(toStrings((Collection<?>) stringSet));
        }

        Object numberSet = read(attributeValue, "ns");
        if (isPresent(attributeValue, "hasNs", numberSet) && numberSet instanceof Collection<?>) {
            return DynamoDbQueryValue.numberSet(toStrings((Collection<?>) numberSet));
        }

        Object binarySet = read(attributeValue, "bs");
        if (isPresent(attributeValue, "hasBs", binarySet) && binarySet instanceof Collection<?>) {
            List<byte[]> binaries = new ArrayList<>();
            for (Object binary : (Collection<?>) binarySet) {
                binaries.add(snapshotBinary(binary));
            }
            return DynamoDbQueryValue.binarySet(binaries);
        }

        Object binary = read(attributeValue, "b");
        if (binary != null) {
            return DynamoDbQueryValue.binary(snapshotBinary(binary));
        }
        return DynamoDbQueryValue.unset();
    }

    /** Captures key maps from attribute names to typed values in source order, or null for a non-collection. */
    private static List<Map<String, DynamoDbQueryValue>> snapshotBatchKeys(Object source) {
        if (!(source instanceof Collection<?>)) {
            return null;
        }
        List<Map<String, DynamoDbQueryValue>> result = new ArrayList<>();
        for (Object rawKey : (Collection<?>) source) {
            Map<String, DynamoDbQueryValue> key = snapshotAttributeMap(rawKey);
            if (key != null) {
                result.add(key);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** Captures a map from attribute names to typed values, or null when the source is not a map. */
    private static Map<String, DynamoDbQueryValue> snapshotAttributeMap(Object source) {
        if (!(source instanceof Map<?, ?>)) {
            return null;
        }
        Map<String, DynamoDbQueryValue> result = new LinkedHashMap<>();
        ((Map<?, ?>) source).forEach((key, value) -> {
            if (key != null) {
                result.put(String.valueOf(key), value == null
                        ? DynamoDbQueryValue.unset()
                        : snapshotAttributeValue(value));
            }
        });
        return Collections.unmodifiableMap(result);
    }

    private static List<DynamoDbQueryValue> snapshotAttributeValues(Collection<?> source) {
        List<DynamoDbQueryValue> result = new ArrayList<>(source.size());
        for (Object value : source) {
            result.add(value == null ? DynamoDbQueryValue.unset() : snapshotAttributeValue(value));
        }
        return result;
    }

    private static List<String> toStrings(Collection<?> source) {
        List<String> result = new ArrayList<>(source.size());
        for (Object value : source) {
            result.add(value == null ? null : String.valueOf(value));
        }
        return result;
    }

    private static boolean isPresent(Object target, String hasMethod, Object value) {
        Object hasValue = read(target, hasMethod);
        if (hasValue instanceof Boolean) {
            return (Boolean) hasValue;
        }
        if (value instanceof Collection<?>) {
            return !((Collection<?>) value).isEmpty();
        }
        if (value instanceof Map<?, ?>) {
            return !((Map<?, ?>) value).isEmpty();
        }
        return value != null;
    }

    private static byte[] snapshotBinary(Object value) {
        if (value instanceof byte[]) {
            return ((byte[]) value).clone();
        }
        if (!(value instanceof ByteBuffer)) {
            Object bytes = read(value, "asByteArray");
            if (bytes instanceof byte[]) {
                return ((byte[]) bytes).clone();
            }
            value = read(value, "asByteBuffer");
        }
        if (!(value instanceof ByteBuffer)) {
            return new byte[0];
        }
        ByteBuffer buffer = ((ByteBuffer) value).asReadOnlyBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    private static Object read(Object target, String methodName) {
        return DynamoDbReflectionHelper.invokeNoArgPropagatingRuntimeExceptions(target, methodName);
    }

    private static String readString(Object target, String methodName) {
        Object value = read(target, methodName);
        return value == null ? null : String.valueOf(value);
    }

    /**
     * Serializable representation of the batch-get request keys consumed by the parser.
     */
    public static final class BatchGetKeys implements Serializable {

        /** Each list entry maps a key attribute name to its typed value. */
        private final List<Map<String, DynamoDbQueryValue>> keys;

        private BatchGetKeys(Object keysAndAttributes) {
            keys = snapshotBatchKeys(read(keysAndAttributes, "keys"));
        }

        /**
         * Returns normalized batch-get keys.
         *
         * @return list of attribute name to typed key value maps, or {@code null}
         */
        public List<Map<String, DynamoDbQueryValue>> keys() {
            return keys;
        }
    }

}
