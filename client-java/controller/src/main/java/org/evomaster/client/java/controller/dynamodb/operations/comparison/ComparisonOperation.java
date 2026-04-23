package org.evomaster.client.java.controller.dynamodb.operations.comparison;

import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;

/**
 * Base class for comparison operations over a field and value.
 *
 * @param <V> value type
 */
public abstract class ComparisonOperation<V> extends QueryOperation {

    private final String fieldName;
    private final V value;

    /**
     * Creates a comparison operation.
     *
     * @param fieldName field name or path
     * @param value comparison value
     */
    ComparisonOperation(String fieldName, V value) {
        this.fieldName = fieldName;
        this.value = value;
    }

    public String getFieldName() {
        return fieldName;
    }

    public V getValue() {
        return value;
    }
}
