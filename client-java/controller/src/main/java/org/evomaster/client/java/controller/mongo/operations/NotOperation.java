package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represent $not operation.
 * Selects the documents that do not match the condition.
 */
public class NotOperation extends QueryOperation {
    private final String fieldName;
    private final QueryOperation condition;

    public NotOperation(String fieldName, QueryOperation condition) {
        this.fieldName = fieldName;
        this.condition = condition;
    }

    public String getFieldName() {
        return fieldName;
    }

    public QueryOperation getCondition() {
        return condition;
    }
}