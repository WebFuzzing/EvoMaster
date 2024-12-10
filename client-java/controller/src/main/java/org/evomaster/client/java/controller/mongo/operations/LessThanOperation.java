package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represent $lt operation.
 * Selects the documents where the value of the field is less than to a specified value
 */
public class LessThanOperation<V> extends ComparisonOperation<V>{
    public LessThanOperation(String fieldName, V value) {
        super(fieldName, value);
    }
}