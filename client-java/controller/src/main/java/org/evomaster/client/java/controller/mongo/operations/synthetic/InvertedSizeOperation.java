package org.evomaster.client.java.controller.mongo.operations.synthetic;

import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

/**
 * Represent the operation that results from applying a $not to a $size operation.
 * It's synthetic as there is no operator defined in the spec with this behaviour.
 * Selects the documents that do not match the $size operation.
 */
public class InvertedSizeOperation extends QueryOperation {
    private final String fieldName;
    private final Integer value;

    public InvertedSizeOperation(String fieldName, Integer value) {
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