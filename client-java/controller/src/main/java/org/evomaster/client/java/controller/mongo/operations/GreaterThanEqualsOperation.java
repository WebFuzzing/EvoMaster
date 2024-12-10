package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represent $gte operation.
 * Selects the documents where the value of the field is greater than or equal to a specified value
 */
public class GreaterThanEqualsOperation<V> extends ComparisonOperation<V>{
    public GreaterThanEqualsOperation(String fieldName, V value) {
        super(fieldName, value);
    }
}