package org.evomaster.client.java.controller.dynamodb.operations;

public class ContainsOperation extends QueryOperation {

    private final String fieldName;
    private final Object expectedValue;

    public ContainsOperation(String fieldName, Object expectedValue) {
        this.fieldName = fieldName;
        this.expectedValue = expectedValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public Object getExpectedValue() {
        return expectedValue;
    }
}
