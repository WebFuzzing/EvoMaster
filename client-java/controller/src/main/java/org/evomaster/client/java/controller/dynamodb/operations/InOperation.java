package org.evomaster.client.java.controller.dynamodb.operations;

import java.util.List;

/**
 * DynamoDB {@code path IN (...)} predicate operation.
 *
 * @param <V> value type
 */
public class InOperation<V> extends QueryOperation {

    private final String fieldName;
    private final List<V> values;

    /**
     * Creates an IN operation.
     *
     * @param fieldName attribute path
     * @param values candidate values
     */
    public InOperation(String fieldName, List<V> values) {
        this.fieldName = fieldName;
        this.values = values;
    }

    public String getFieldName() {
        return fieldName;
    }

    public List<V> getValues() {
        return values;
    }
}
