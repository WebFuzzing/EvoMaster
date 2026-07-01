package org.evomaster.client.java.controller.cassandra.operations;

/**
 * Represents a CQL CONTAINS operation.
 * The CONTAINS operator may only be used for collection columns (lists, sets, and maps).
 * In the case of maps, CONTAINS applies to the map values.
 */
public class ContainsOperation<V> extends CqlQueryOperation {
    private final String columnName;
    private final V value;

    public ContainsOperation(String columnName, V value) {
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
