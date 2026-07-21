package org.evomaster.client.java.controller.mongo.operations;

public abstract class ComparisonOperation<V> extends QueryOperationWithField {
    private final V value;

    ComparisonOperation(String fieldName, V value) {
        super(fieldName);
        this.value = value;
    }

    public V getValue() {
        return value;
    }
}
