package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.controller.dynamodb.ParsedDynamoDbRequest;
import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;
import org.evomaster.client.java.instrumentation.DynamoDbQuery;

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
    public Map<String, ParsedDynamoDbRequest> parseRequest(DynamoDbQuery ddbQuery) {
        String tableName = readValidTableName(ddbQuery);
        if (tableName == null) {
            return Collections.emptyMap();
        }

        Map<String, String> names = readNameMap(ddbQuery);
        Map<String, Object> values = readValueMap(ddbQuery);

        QueryOperation keyCondition = parseExpression(
                ddbQuery.keyConditionExpression(),
                names,
                values
        );
        QueryOperation filterExpression = parseExpression(
                ddbQuery.filterExpression(),
                names,
                values
        );

        return buildSingleTableRequestMap(tableName, keyCondition, filterExpression);
    }
}
