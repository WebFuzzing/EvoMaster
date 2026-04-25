package org.evomaster.client.java.controller.dynamodb.operations.comparison;

/**
 * Equality comparison operation ({@code =}).
 *
 * @param <V> value type
 */
public class EqualsOperation<V> extends ComparisonOperation<V> {

    /**
     * Creates an equality comparison operation.
     *
     * @param fieldName field name coming from DynamoDB expression/condition
     * @param value comparison value
     */
    public EqualsOperation(String fieldName, V value) {
        super(fieldName, value);
    }
}
