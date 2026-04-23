package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;

import java.util.Collections;
import java.util.Map;

abstract class WriteMethodParser extends DynamoDbBaseApiMethodParser {

    protected abstract boolean requiresKeyCondition();

    @Override
    public final Map<String, QueryOperation> parseRequest(Object request) {
        String tableName = readValidTableName(request);
        if (tableName == null) {
            return Collections.emptyMap();
        }

        QueryOperation keyCondition = requiresKeyCondition() ? parseKeyCondition(request) : null;
        QueryOperation conditionExpression = parseExpression(
                readString(request, METHOD_CONDITION_EXPRESSION),
                readNameMap(request),
                readValueMap(request)
        );

        return singleTableResult(tableName, combineWithAnd(keyCondition, conditionExpression));
    }
}
