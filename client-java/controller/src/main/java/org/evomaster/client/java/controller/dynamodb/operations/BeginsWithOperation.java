package org.evomaster.client.java.controller.dynamodb.operations;

public class BeginsWithOperation extends QueryOperation {

    private final String fieldName;
    private final Object prefix;

    public BeginsWithOperation(String fieldName, Object prefix) {
        this.fieldName = fieldName;
        this.prefix = prefix;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getPrefix() {
        return prefix;
    }
}
