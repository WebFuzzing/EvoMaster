package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represent $ne operation.
 * Selects the documents where the value of the field is not equal to the specified value.
 * This includes documents that do not contain the field.
 */
public class NotEqualsOperation<V> extends ComparisonOperation<V>{
    public NotEqualsOperation(String fieldName, V value) {
        super(fieldName, value);
    }
}