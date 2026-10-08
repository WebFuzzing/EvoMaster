package org.evomaster.client.java.controller.cassandra.operations;

/**
 * Represents a CQL greater-than-or-equals comparison operation ({@code column >= value}).
 */
public class GreaterThanEqualsOperation<V> extends ComparisonOperation<V> {
    public GreaterThanEqualsOperation(String columnName, V value) {
        super(columnName, value);
    }
}
