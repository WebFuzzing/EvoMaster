package org.evomaster.client.java.controller.dynamodb.operations;

import java.util.List;

/**
 * Logical AND operation over multiple DynamoDB query conditions.
 */
public class AndOperation extends QueryOperation {

    private final List<QueryOperation> conditions;

    /**
     * Creates an AND operation.
     *
     * @param conditions conditions to combine
     */
    public AndOperation(List<QueryOperation> conditions) {
        this.conditions = conditions;
    }

    /**
     * Returns the conditions combined by this AND operation.
     *
     * @return combined conditions
     */
    public List<QueryOperation> getConditions() {
        return conditions;
    }
}
