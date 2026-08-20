package org.evomaster.client.java.controller.cassandra.operations;

/**
 * Represents a CQL less-than-or-equals comparison operation ({@code column <= value}).
 */
public class LessThanEqualsOperation<V> extends ComparisonOperation<V> {
    public LessThanEqualsOperation(String columnName, V value) {
        super(columnName, value);
    }
}
