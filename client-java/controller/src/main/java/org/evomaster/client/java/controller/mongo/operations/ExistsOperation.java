package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represent $exists operation.
 * When boolean is true, $exists matches the documents that contain the field.
 * When boolean is false, the query returns only the documents that do not contain the field.
 */
public class ExistsOperation extends QueryOperation{
    private final String fieldName;
    private final Boolean bool;

    public ExistsOperation(String fieldName, Boolean bool) {
        this.fieldName = fieldName;
        this.bool = bool;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Boolean getBoolean() {
        return bool;
    }
}