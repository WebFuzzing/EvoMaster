package org.evomaster.client.java.controller.dynamodb.operations;

public class ExistsOperation extends QueryOperation {

    private final String fieldName;
    //true = exists, false = not exists
    private final boolean exists;

    public ExistsOperation(String fieldName, boolean exists) {
        this.fieldName = fieldName;
        this.exists = exists;
    }

    public String getFieldName() {
        return fieldName;
    }

    public boolean isExists() {
        return exists;
    }
}
