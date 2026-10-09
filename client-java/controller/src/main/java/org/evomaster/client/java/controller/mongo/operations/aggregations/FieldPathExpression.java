package org.evomaster.client.java.controller.mongo.operations.aggregations;

import java.util.Objects;

/**
 * Represents the value of a field of the document, written like "$field.nested".
 */
public class FieldPathExpression extends AggregationExpression {

    private final String fieldPath;

    /**
     * @param fieldPath the dot-separated path of the field, without the dollar sign
     */
    public FieldPathExpression(String fieldPath) {
        this.fieldPath = Objects.requireNonNull(fieldPath);
    }

    public String getFieldPath() {
        return fieldPath;
    }
}
