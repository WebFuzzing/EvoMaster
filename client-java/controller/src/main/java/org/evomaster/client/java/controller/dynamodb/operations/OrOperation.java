package org.evomaster.client.java.controller.dynamodb.operations;

import java.util.List;

/**
 * Logical OR operation over multiple DynamoDB query conditions.
 */
public class OrOperation extends QueryOperation {

    private final List<QueryOperation> conditions;

    /**
     * Creates an OR operation.
     *
     * @param conditions conditions to combine
     */
    public OrOperation(List<QueryOperation> conditions) {
        this.conditions = conditions;
    }

    /**
     * Returns the conditions combined by this OR operation.
     *
     * @return combined conditions
     */
    public List<QueryOperation> getConditions() {
        return conditions;
    }
}
