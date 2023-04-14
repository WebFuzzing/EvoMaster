package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represent $gt operation.
 * Selects the documents where the value of the field is greater than or equal to a specified value
 */
public class GreaterThanOperation<V> extends ComparisonOperation<V>{
    public GreaterThanOperation(String fieldName, V value) {
        super(fieldName, value);
    }
}