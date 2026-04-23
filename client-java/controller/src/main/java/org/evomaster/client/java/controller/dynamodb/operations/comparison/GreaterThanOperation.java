package org.evomaster.client.java.controller.dynamodb.operations.comparison;

public class GreaterThanOperation<V> extends ComparisonOperation<V> {

    public GreaterThanOperation(String fieldName, V value) {
        super(fieldName, value);
    }
}
