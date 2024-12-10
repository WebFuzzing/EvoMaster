package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represent $eq operation.
 * Matches documents where the value of a field equals the specified value.
 */
public class EqualsOperation<V> extends ComparisonOperation<V>{
    public EqualsOperation(String fieldName, V value) {
        super(fieldName, value);
    }
}