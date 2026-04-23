package org.evomaster.client.java.controller.dynamodb.operations.comparison;

import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;

public abstract class ComparisonOperation<V> extends QueryOperation {

    private final String fieldName;
    private final V value;

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
