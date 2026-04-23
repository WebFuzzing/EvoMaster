package org.evomaster.client.java.controller.dynamodb.operations;

public class NotOperation extends QueryOperation {

    private final QueryOperation condition;

    public NotOperation(QueryOperation condition) {
        this.condition = condition;
    }

    public QueryOperation getCondition() {
        return condition;
    }
}
