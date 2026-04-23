package org.evomaster.client.java.controller.dynamodb.operations.comparison;

public class GreaterThanEqualsOperation<V> extends ComparisonOperation<V> {

    public GreaterThanEqualsOperation(String fieldName, V value) {
        super(fieldName, value);
    }
}
