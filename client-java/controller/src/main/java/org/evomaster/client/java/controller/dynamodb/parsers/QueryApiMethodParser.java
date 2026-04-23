package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;

import java.util.Collections;
import java.util.Map;

/**
 * Parser for DynamoDB {@code Query} requests.
 */
public class QueryApiMethodParser extends DynamoDbBaseApiMethodParser {

    /**
     * {@inheritDoc}
     */
    @Override
    public DynamoDbOperationNames apiMethodName() {
        return DynamoDbOperationNames.QUERY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, QueryOperation> parseRequest(Object request) {
        String tableName = readValidTableName(request);
        if (tableName == null) {
            return Collections.emptyMap();
        }

        Map<String, String> names = readNameMap(request);
        Map<String, Object> values = readValueMap(request);

        QueryOperation keyCondition = parseExpression(
                readString(request, METHOD_KEY_CONDITION_EXPRESSION),
                names,
                values
        );
        QueryOperation filterCondition = parseExpression(
                readString(request, METHOD_FILTER_EXPRESSION),
                names,
                values
        );

        return singleTableResult(tableName, combineWithAnd(keyCondition, filterCondition));
    }
}
