package org.evomaster.client.java.controller.cassandra.operations;

public class GreaterThanOperation<V> extends ComparisonOperation<V> {
    public GreaterThanOperation(String columnName, V value) {
        super(columnName, value);
    }
}
