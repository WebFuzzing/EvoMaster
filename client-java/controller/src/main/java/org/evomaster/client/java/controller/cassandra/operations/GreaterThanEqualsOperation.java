package org.evomaster.client.java.controller.cassandra.operations;

public class GreaterThanEqualsOperation<V> extends ComparisonOperation<V> {
    public GreaterThanEqualsOperation(String columnName, V value) {
        super(columnName, value);
    }
}
