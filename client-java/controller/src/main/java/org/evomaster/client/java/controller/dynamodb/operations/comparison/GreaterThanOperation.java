package org.evomaster.client.java.controller.dynamodb.operations.comparison;

/**
 * Greater-than comparison operation ({@code >}).
 *
 * @param <V> value type
 */
public class GreaterThanOperation<V> extends ComparisonOperation<V> {

    /**
     * Creates a greater-than comparison operation.
     *
     * @param fieldName field name coming from DynamoDB expression/condition
     * @param value comparison value
     */
    public GreaterThanOperation(String fieldName, V value) {
        super(fieldName, value);
    }
}
