package org.evomaster.client.java.controller.dynamodb.operations.comparison;

/**
 * Less-than comparison operation ({@code <}).
 *
 * @param <V> value type
 */
public class LessThanOperation<V> extends ComparisonOperation<V> {

    /**
     * Creates a less-than comparison operation.
     *
     * @param fieldName field name coming from DynamoDB expression/condition
     * @param value comparison value
     */
    public LessThanOperation(String fieldName, V value) {
        super(fieldName, value);
    }
}
