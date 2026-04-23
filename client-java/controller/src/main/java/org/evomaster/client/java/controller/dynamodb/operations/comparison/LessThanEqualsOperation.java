package org.evomaster.client.java.controller.dynamodb.operations.comparison;

/**
 * Less-than-or-equals comparison operation ({@code <=}).
 *
 * @param <V> value type
 */
public class LessThanEqualsOperation<V> extends ComparisonOperation<V> {

    /**
     * Creates a less-than-or-equals comparison operation.
     *
     * @param fieldName field name or path
     * @param value comparison value
     */
    public LessThanEqualsOperation(String fieldName, V value) {
        super(fieldName, value);
    }
}
