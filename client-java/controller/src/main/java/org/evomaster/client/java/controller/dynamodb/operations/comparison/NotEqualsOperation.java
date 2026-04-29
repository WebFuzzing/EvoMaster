package org.evomaster.client.java.controller.dynamodb.operations.comparison;

/**
 * Inequality comparison operation ({@code <>}).
 *
 * @param <V> value type
 */
public class NotEqualsOperation<V> extends ComparisonOperation<V> {

    /**
     * Creates an inequality comparison operation.
     *
     * @param fieldName field name coming from DynamoDB expression/condition
     * @param value comparison value
     */
    public NotEqualsOperation(String fieldName, V value) {
        super(fieldName, value);
    }
}
