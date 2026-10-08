package org.evomaster.client.java.controller.cassandra.operations;

/**
 * Represents a CQL equality comparison operation ({@code column = value}).
 */
public class EqualsOperation<V> extends ComparisonOperation<V> {
    public EqualsOperation(String columnName, V value) {
        super(columnName, value);
    }
}
