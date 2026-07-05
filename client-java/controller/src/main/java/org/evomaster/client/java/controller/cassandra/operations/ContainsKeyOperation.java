package org.evomaster.client.java.controller.cassandra.operations;

/**
 * Represents a CQL CONTAINS KEY operation.
 * The CONTAINS KEY operator may only be used on map columns and applies to the map keys.
 */
public class ContainsKeyOperation<V> extends CqlQueryOperation {
    private final String columnName;
    private final V value;

    public ContainsKeyOperation(String columnName, V value) {
        this.columnName = columnName;
        this.value = value;
    }

    public String getColumnName() {
        return columnName;
    }

    public V getValue() {
        return value;
    }
}
