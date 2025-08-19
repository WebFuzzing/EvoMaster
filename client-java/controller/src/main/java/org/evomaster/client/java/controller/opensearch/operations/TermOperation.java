package org.evomaster.client.java.controller.opensearch.operations;

/**
 * Represent Term operation.
 * Matches documents where the value of a field equals the specified value.
 */
public class TermOperation<V> extends ComparisonOperation<V> {
    public TermOperation(String fieldName, V value) {
        super(fieldName, value);
    }
}