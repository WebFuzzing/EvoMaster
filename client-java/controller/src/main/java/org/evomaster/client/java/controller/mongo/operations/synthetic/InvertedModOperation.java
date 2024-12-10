package org.evomaster.client.java.controller.mongo.operations.synthetic;

import org.evomaster.client.java.controller.mongo.operations.QueryOperation;

/**
 * Represent the operation that results from applying a $not to a $mod operation.
 * It's synthetic as there is no operator defined in the spec with this behaviour.
 * Selects the documents that do not match the $mod operation.
 */
public class InvertedModOperation extends QueryOperation{
    private final String fieldName;
    private final Long divisor;
    private final Long remainder;

    public InvertedModOperation(String fieldName, Long divisor, Long remainder) {
        this.fieldName = fieldName;
        this.divisor = divisor;
        this.remainder = remainder;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Long getDivisor() {
        return divisor;
    }

    public Long getRemainder() {
        return remainder;
    }
}