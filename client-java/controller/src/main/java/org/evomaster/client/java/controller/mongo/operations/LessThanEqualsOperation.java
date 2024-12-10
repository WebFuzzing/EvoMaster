package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represent $lte operation.
 * Selects the documents where the value of the field is less than or equal to a specified value
 */
public class LessThanEqualsOperation<V> extends ComparisonOperation<V>{
    public LessThanEqualsOperation(String fieldName, V value) {
        super(fieldName, value);
    }
}