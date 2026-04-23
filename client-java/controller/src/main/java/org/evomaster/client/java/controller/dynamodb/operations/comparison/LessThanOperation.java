package org.evomaster.client.java.controller.dynamodb.operations.comparison;

public class LessThanOperation<V> extends ComparisonOperation<V> {

    public LessThanOperation(String fieldName, V value) {
        super(fieldName, value);
    }
}
