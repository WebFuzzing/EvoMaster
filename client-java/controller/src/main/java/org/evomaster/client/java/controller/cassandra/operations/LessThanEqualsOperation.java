package org.evomaster.client.java.controller.cassandra.operations;

public class LessThanEqualsOperation<V> extends ComparisonOperation<V> {
    public LessThanEqualsOperation(String columnName, V value) {
        super(columnName, value);
    }
}
