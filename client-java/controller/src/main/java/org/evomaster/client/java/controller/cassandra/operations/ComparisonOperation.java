package org.evomaster.client.java.controller.cassandra.operations;

/**
 * Represents a comparison operation.
 */
abstract public class ComparisonOperation<V> extends CqlQueryOperation {
    private final String columnName;
    private final V value;

    ComparisonOperation(String fieldName, V value) {
        this.columnName = fieldName;
        this.value = value;
    }

    public String getColumnName() {
        return columnName;
    }

    public V getValue() {
        return value;
    }
}
