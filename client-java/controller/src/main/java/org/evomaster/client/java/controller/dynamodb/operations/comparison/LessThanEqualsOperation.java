package org.evomaster.client.java.controller.dynamodb.operations.comparison;

public class LessThanEqualsOperation<V> extends ComparisonOperation<V> {

    public LessThanEqualsOperation(String fieldName, V value) {
        super(fieldName, value);
    }
}
