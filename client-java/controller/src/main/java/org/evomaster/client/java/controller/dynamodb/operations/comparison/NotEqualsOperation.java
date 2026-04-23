package org.evomaster.client.java.controller.dynamodb.operations.comparison;

public class NotEqualsOperation<V> extends ComparisonOperation<V> {

    public NotEqualsOperation(String fieldName, V value) {
        super(fieldName, value);
    }
}
