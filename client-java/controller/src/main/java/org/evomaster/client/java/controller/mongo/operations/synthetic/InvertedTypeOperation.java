package org.evomaster.client.java.controller.mongo.operations.synthetic;

import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

/**
 * Represent the operation that results from applying a $not to a type operation.
 * It's synthetic as there is no operator defined in the spec with this behaviour.
 * Selects the documents that do not match the $type operation.
 */
public class InvertedTypeOperation extends QueryOperation {
    private final String fieldName;
    private final Object type;

    public InvertedTypeOperation(String fieldName, Object type) {
        this.fieldName = fieldName;
        this.type = type;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getType() {
        return type;
    }
}