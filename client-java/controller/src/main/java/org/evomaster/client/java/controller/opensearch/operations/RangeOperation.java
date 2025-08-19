package org.evomaster.client.java.controller.opensearch.operations;

/**
 * Represents a Range operation.
 * Matches documents where the value of a field is in the specified range.
 */
public class RangeOperation<V> extends ComparisonOperation<V> {

    public RangeOperation(String fieldName, V value) {
        super(fieldName, value);
    }
}
