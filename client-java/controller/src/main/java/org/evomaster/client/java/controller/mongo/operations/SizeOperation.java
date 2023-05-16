package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represent $size operation.
 * Matches any array with the number of elements specified by the argument.
 */
public class SizeOperation extends QueryOperation {
    private final String fieldName;
    private final Integer value;

    public SizeOperation(String fieldName, Integer value) {
        this.fieldName = fieldName;
        this.value = value;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Integer getValue() {
        return value;
    }
}