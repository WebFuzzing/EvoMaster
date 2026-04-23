package org.evomaster.client.java.controller.dynamodb.operations.comparison;

public class EqualsOperation<V> extends ComparisonOperation<V> {

    public EqualsOperation(String fieldName, V value) {
        super(fieldName, value);
    }
}
