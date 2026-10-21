package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.controller.dynamodb.ParsedDynamoDbRequest;
import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;
import org.evomaster.client.java.instrumentation.DynamoDbRequestSnapshot;

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
    public Map<String, ParsedDynamoDbRequest> parseRequest(DynamoDbRequestSnapshot ddbRequest) {
        String tableName = readValidTableName(ddbRequest);
        if (tableName == null) {
            return Collections.emptyMap();
        }

        Map<String, String> names = readNameMap(ddbRequest);
        Map<String, Object> values = readValueMap(ddbRequest);

        QueryOperation keyCondition = parseExpression(
                ddbRequest.keyConditionExpression(),
                names,
                values
        );
        QueryOperation filterExpression = parseExpression(
                ddbRequest.filterExpression(),
                names,
                values
        );

        return buildSingleTableRequestMap(tableName, keyCondition, filterExpression);
    }
}
