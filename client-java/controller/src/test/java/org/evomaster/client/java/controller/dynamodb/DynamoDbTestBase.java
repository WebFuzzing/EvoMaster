package org.evomaster.client.java.controller.dynamodb;

import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.controller.dynamodb.operations.comparison.ComparisonOperation;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public abstract class DynamoDbTestBase {

    protected final Map<String, Object> values(Object... kv) {
        return toMap(kv, value -> value);
    }

    protected final Map<String, String> names(Object... kv) {
        return toMap(kv, String::valueOf);
    }

    protected final Map<String, AttributeValue> attributeValues(Object... kv) {
        return toMap(kv, value -> (AttributeValue) value);
    }

    protected final AttributeValue stringValue(String value) {
        return AttributeValue.builder().s(value).build();
    }

    protected final AttributeValue numberValue(String value) {
        return AttributeValue.builder().n(value).build();
    }

    @SuppressWarnings({"rawtypes"})
    protected final void assertComparison(
            QueryOperation operation,
            Class<? extends ComparisonOperation> expectedType,
            String expectedField,
            Object expectedValue) {
        assertNotNull(operation);
        assertTrue(expectedType.isInstance(operation));
        ComparisonOperation<?> comparison = (ComparisonOperation<?>) operation;
        assertEquals(expectedField, comparison.getFieldName());
        assertEquals(expectedValue, comparison.getValue());
    }

    protected final <T> T castAs(QueryOperation operation, Class<T> type) {
        assertNotNull(operation);
        assertTrue(type.isInstance(operation));
        return type.cast(operation);
    }

    private static <V> Map<String, V> toMap(Object[] kv, Function<Object, V> valueMapper) {
        if (kv.length % 2 != 0) {
            throw new IllegalArgumentException("Expected an even number of key/value arguments");
        }

        Map<String, V> map = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put(String.valueOf(kv[i]), valueMapper.apply(kv[i + 1]));
        }
        return map;
    }
}
