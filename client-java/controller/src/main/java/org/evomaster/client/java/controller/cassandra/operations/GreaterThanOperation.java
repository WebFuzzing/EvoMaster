package org.evomaster.client.java.controller.cassandra.operations;

/**
 * Represents a CQL greater-than comparison operation ({@code column > value}).
 */
public class GreaterThanOperation<V> extends ComparisonOperation<V> {
    public GreaterThanOperation(String columnName, V value) {
        super(columnName, value);
    }
}
