package org.evomaster.client.java.instrumentation;

import org.evomaster.client.java.controller.dynamodb.DynamoDbReflectionHelper;

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
 * Stores a Serializable subset of DynamoDB SDK request fields consumed by EvoMaster's request parsers.
 */
public final class DynamoDbRequestSnapshot implements Serializable {

    private final String tableName;
    private final Map<String, DynamoDbValueSnapshot> key;
    private final Map<String, KeysAndAttributesSnapshot> batchRequestItems;
    private final String keyConditionExpression;
    private final String filterExpression;
    private final Map<String, String> expressionAttributeNames;
    private final Map<String, DynamoDbValueSnapshot> expressionAttributeValues;

    private DynamoDbRequestSnapshot(Object request) {
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
     * Creates a serializable snapshot containing the fields required by the supported request parsers.
     *
     * @param request original SDK request
     * @return serializable snapshot, or {@code null} when the request is null
     */
    public static DynamoDbRequestSnapshot from(Object request) {
        return request == null ? null : new DynamoDbRequestSnapshot(request);
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
     * @return key map, or {@code null}
     */
    public Map<String, DynamoDbValueSnapshot> key() {
        return key;
    }

    /**
     * Returns batch request items by table.
     *
     * @return request item map, or {@code null}
     */
    public Map<String, KeysAndAttributesSnapshot> batchRequestItems() {
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
     * @return normalized name map
     */
    public Map<String, String> expressionAttributeNames() {
        return expressionAttributeNames;
    }

    /**
     * Returns the expression attribute values.
     *
     * @return normalized value map
     */
    public Map<String, DynamoDbValueSnapshot> expressionAttributeValues() {
        return expressionAttributeValues;
    }

    /**
     * Returns the predicate shape used to group heuristic entries for one table.
     * Attribute values and key values are excluded, so equivalent predicates share an identifier.
     *
     * @param batchGetTableName table whose batch-get key fields should be included
     * @return predicate expressions, name aliases, and sorted key field names
     */
    public String toString(String batchGetTableName) {
        Set<String> keyFields = new TreeSet<>();
        if (key != null) {
            keyFields.addAll(key.keySet());
        }
        if (batchRequestItems != null) {
            KeysAndAttributesSnapshot tableItems = batchRequestItems.get(batchGetTableName);
            if (tableItems != null && tableItems.keys() != null) {
                for (Map<String, DynamoDbValueSnapshot> batchKey : tableItems.keys()) {
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

    private static Map<String, KeysAndAttributesSnapshot> snapshotRequestItems(Object requestItems) {
        if (!(requestItems instanceof Map<?, ?>)) {
            return null;
        }

        Map<String, KeysAndAttributesSnapshot> result = new LinkedHashMap<>();
        ((Map<?, ?>) requestItems).forEach((table, keysAndAttributes) -> {
            if (table != null) {
                result.put(String.valueOf(table), new KeysAndAttributesSnapshot(keysAndAttributes));
            }
        });
        return Collections.unmodifiableMap(result);
    }

    /** Returns a serializable copy of non-null expression name aliases. */
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

    private static DynamoDbValueSnapshot snapshotAttributeValue(Object attributeValue) {
        Object nul = read(attributeValue, "nul");
        if (Boolean.TRUE.equals(nul)) {
            return DynamoDbValueSnapshot.nullValue();
        }

        Object stringValue = read(attributeValue, "s");
        if (stringValue instanceof String) {
            return DynamoDbValueSnapshot.string((String) stringValue);
        }

        Object numberValue = read(attributeValue, "n");
        if (numberValue instanceof String && !((String) numberValue).isEmpty()) {
            return DynamoDbValueSnapshot.number((String) numberValue);
        }

        Object booleanValue = read(attributeValue, "bool");
        if (booleanValue instanceof Boolean) {
            return DynamoDbValueSnapshot.bool((Boolean) booleanValue);
        }

        Object mapValue = read(attributeValue, "m");
        if (isPresent(attributeValue, "hasM", mapValue) && mapValue instanceof Map<?, ?>) {
            return DynamoDbValueSnapshot.map(snapshotAttributeMap(mapValue));
        }

        Object listValue = read(attributeValue, "l");
        if (isPresent(attributeValue, "hasL", listValue) && listValue instanceof Collection<?>) {
            return DynamoDbValueSnapshot.list(snapshotAttributeValues((Collection<?>) listValue));
        }

        Object stringSet = read(attributeValue, "ss");
        if (isPresent(attributeValue, "hasSs", stringSet) && stringSet instanceof Collection<?>) {
            return DynamoDbValueSnapshot.stringSet(toStrings((Collection<?>) stringSet));
        }

        Object numberSet = read(attributeValue, "ns");
        if (isPresent(attributeValue, "hasNs", numberSet) && numberSet instanceof Collection<?>) {
            return DynamoDbValueSnapshot.numberSet(toStrings((Collection<?>) numberSet));
        }

        Object binarySet = read(attributeValue, "bs");
        if (isPresent(attributeValue, "hasBs", binarySet) && binarySet instanceof Collection<?>) {
            List<byte[]> binaries = new ArrayList<>();
            for (Object binary : (Collection<?>) binarySet) {
                binaries.add(snapshotBinary(binary));
            }
            return DynamoDbValueSnapshot.binarySet(binaries);
        }

        Object binary = read(attributeValue, "b");
        if (binary != null) {
            return DynamoDbValueSnapshot.binary(snapshotBinary(binary));
        }
        return DynamoDbValueSnapshot.unset();
    }

    /** Captures valid batch-get key maps in source order, or null for a non-collection. */
    private static List<Map<String, DynamoDbValueSnapshot>> snapshotBatchKeys(Object source) {
        if (!(source instanceof Collection<?>)) {
            return null;
        }
        List<Map<String, DynamoDbValueSnapshot>> result = new ArrayList<>();
        for (Object rawKey : (Collection<?>) source) {
            Map<String, DynamoDbValueSnapshot> key = snapshotAttributeMap(rawKey);
            if (key != null) {
                result.add(key);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /** Captures a typed attribute map, or null when the source is not a map. */
    private static Map<String, DynamoDbValueSnapshot> snapshotAttributeMap(Object source) {
        if (!(source instanceof Map<?, ?>)) {
            return null;
        }
        Map<String, DynamoDbValueSnapshot> result = new LinkedHashMap<>();
        ((Map<?, ?>) source).forEach((key, value) -> {
            if (key != null) {
                result.put(String.valueOf(key), value == null
                        ? DynamoDbValueSnapshot.unset()
                        : snapshotAttributeValue(value));
            }
        });
        return Collections.unmodifiableMap(result);
    }

    private static List<DynamoDbValueSnapshot> snapshotAttributeValues(Collection<?> source) {
        List<DynamoDbValueSnapshot> result = new ArrayList<>(source.size());
        for (Object value : source) {
            result.add(value == null ? DynamoDbValueSnapshot.unset() : snapshotAttributeValue(value));
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
    public static final class KeysAndAttributesSnapshot implements Serializable {

        private final List<Map<String, DynamoDbValueSnapshot>> keys;

        private KeysAndAttributesSnapshot(Object keysAndAttributes) {
            keys = snapshotBatchKeys(read(keysAndAttributes, "keys"));
        }

        /**
         * Returns normalized batch-get keys.
         *
         * @return key list, or {@code null}
         */
        public List<Map<String, DynamoDbValueSnapshot>> keys() {
            return keys;
        }
    }

}
