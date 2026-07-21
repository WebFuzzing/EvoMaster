package org.evomaster.client.java.controller.mongo.operations;

/**
 * Represent $not operation.
 * Selects the documents that do not match the condition.
 */
public class NotOperation extends QueryOperationWithField {
    private final QueryOperation condition;

    public NotOperation(String fieldName, QueryOperation condition) {
        super(fieldName);
        this.condition = condition;
    }

    public QueryOperation getCondition() {
        return condition;
    }
}
