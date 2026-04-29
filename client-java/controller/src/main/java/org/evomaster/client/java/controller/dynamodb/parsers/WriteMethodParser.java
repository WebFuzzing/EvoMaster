package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;

import java.util.Collections;
import java.util.Map;

/**
 * Base parser for write APIs with optional key and condition expressions.
 */
abstract class WriteMethodParser extends DynamoDbBaseApiMethodParser {

    /**
     * Indicates whether the write method requires key conditions.
     *
     * @return {@code true} if key conditions are required
     */
    protected abstract boolean requiresKeyCondition();

    /**
     * {@inheritDoc}
     */
    @Override
    public final Map<String, QueryOperation> parseRequest(Object ddbRequest) {
        String tableName = readValidTableName(ddbRequest);
        if (tableName == null) {
            return Collections.emptyMap();
        }

        QueryOperation keyCondition = requiresKeyCondition() ? parseKeyCondition(ddbRequest) : null;
        QueryOperation conditionExpression = parseExpression(
                readString(ddbRequest, METHOD_CONDITION_EXPRESSION),
                readNameMap(ddbRequest),
                readValueMap(ddbRequest)
        );

        return singleTableResult(tableName, combineWithAnd(keyCondition, conditionExpression));
    }
}
