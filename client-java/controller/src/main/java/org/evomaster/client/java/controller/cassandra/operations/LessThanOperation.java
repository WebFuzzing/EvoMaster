package org.evomaster.client.java.controller.cassandra.operations;

public class LessThanOperation<V> extends ComparisonOperation<V> {
    public LessThanOperation(String columnName, V value) {
        super(columnName, value);
    }
}
