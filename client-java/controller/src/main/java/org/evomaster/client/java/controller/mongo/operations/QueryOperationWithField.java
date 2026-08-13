package org.evomaster.client.java.controller.mongo.operations;

import java.util.Objects;

public abstract class QueryOperationWithField extends QueryOperation {

    private final String fieldName;

    public QueryOperationWithField(String fieldName) {
        Objects.requireNonNull(fieldName);

        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }
}
