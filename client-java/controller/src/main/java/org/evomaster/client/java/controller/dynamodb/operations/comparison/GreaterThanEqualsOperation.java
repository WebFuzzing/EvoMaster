package org.evomaster.client.java.controller.dynamodb.operations.comparison;

/**
 * Greater-than-or-equals comparison operation ({@code >=}).
 *
 * @param <V> value type
 */
public class GreaterThanEqualsOperation<V> extends ComparisonOperation<V> {

    /**
     * Creates a greater-than-or-equals comparison operation.
     *
     * @param fieldName field name or path
     * @param value comparison value
     */
    public GreaterThanEqualsOperation(String fieldName, V value) {
        super(fieldName, value);
    }
}
