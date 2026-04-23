package org.evomaster.client.java.controller.dynamodb.operations;

public class TypeOperation extends QueryOperation {

    private final String fieldName;
    private final String expectedType;

    public TypeOperation(String fieldName, String expectedType) {
        this.fieldName = fieldName;
        this.expectedType = expectedType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getExpectedType() {
        return expectedType;
    }
}
