package org.evomaster.client.java.controller.dynamodb;

import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;

/**
 * Parsed DynamoDB request expressions for one table. Needed in order to keep key and filter expressions separated which
 * will be useful when implementing the heuristic.
 */
public final class ParsedDynamoDbRequest {

    private final QueryOperation keyCondition;
    private final QueryOperation filterExpression;

    /**
     * Creates a parsed DynamoDB request.
     *
     * @param keyCondition parsed key condition, or {@code null}
     * @param filterExpression parsed filter or condition expression, or {@code null}
     */
    public ParsedDynamoDbRequest(QueryOperation keyCondition, QueryOperation filterExpression) {
        this.keyCondition = keyCondition;
        this.filterExpression = filterExpression;
    }

    /**
     * Returns the parsed key condition.
     *
     * @return parsed key condition, or {@code null}
     */
    public QueryOperation getKeyCondition() {
        return keyCondition;
    }

    /**
     * Returns the parsed filter expression.
     *
     * @return parsed filter or condition expression, or {@code null}
     */
    public QueryOperation getFilterExpression() {
        return filterExpression;
    }
}
