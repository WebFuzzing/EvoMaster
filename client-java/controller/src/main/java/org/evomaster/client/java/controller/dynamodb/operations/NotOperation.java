package org.evomaster.client.java.controller.dynamodb.operations;

/**
 * Logical NOT operation over one DynamoDB query condition.
 */
public class NotOperation extends QueryOperation {

    private final QueryOperation condition;

    /**
     * Creates a NOT operation.
     *
     * @param condition condition to negate
     */
    public NotOperation(QueryOperation condition) {
        this.condition = condition;
    }

    public QueryOperation getCondition() {
        return condition;
    }
}
