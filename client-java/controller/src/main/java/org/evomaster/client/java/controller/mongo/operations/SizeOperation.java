package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represent $size operation.
 * Matches any array with the number of elements specified by the argument.
 */
public class SizeOperation extends QueryOperationWithField {
    private final Integer value;

    public SizeOperation(String fieldName, Integer value) {
        super(fieldName);
        this.value = value;
    }

    public Integer getValue() {
        return value;
    }
}
