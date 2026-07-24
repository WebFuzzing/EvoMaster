package org.evomaster.client.java.controller.cassandra.operations;

/**
 * Represents a CQL less-than comparison operation ({@code column < value}).
 */
public class LessThanOperation<V> extends ComparisonOperation<V> {
    public LessThanOperation(String columnName, V value) {
        super(columnName, value);
    }
}
