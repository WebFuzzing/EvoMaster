package org.evomaster.client.java.controller.cassandra.operations;

public class EqualsOperation<V> extends ComparisonOperation<V> {
    public EqualsOperation(String columnName, V value) {
        super(columnName, value);
    }
}
