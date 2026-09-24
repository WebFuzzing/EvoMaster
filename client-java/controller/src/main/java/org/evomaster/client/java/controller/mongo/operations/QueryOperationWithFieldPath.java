package org.evomaster.client.java.controller.mongo.operations;

import java.util.Objects;

public abstract class QueryOperationWithFieldPath extends QueryOperation {

    /**
     * The field path to which the operation applies.
     * This is a dot-separated string that specifies the path to the field in the document.
     */
    private final String fieldPath;

    public QueryOperationWithFieldPath(String fieldPath) {
        Objects.requireNonNull(fieldPath);

        this.fieldPath = fieldPath;
    }

    public String getFieldPath() {
        return fieldPath;
    }
}
