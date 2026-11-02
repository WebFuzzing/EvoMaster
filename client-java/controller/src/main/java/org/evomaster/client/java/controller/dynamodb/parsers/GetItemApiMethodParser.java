package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.controller.dynamodb.ParsedDynamoDbRequest;
import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbOperationNames;
import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbQuery;

import java.util.Map;

/**
 * Parser for DynamoDB {@code GetItem} requests.
 */
public class GetItemApiMethodParser extends DynamoDbBaseApiMethodParser {

    /**
     * {@inheritDoc}
     */
    @Override
    public DynamoDbOperationNames apiMethodName() {
        return DynamoDbOperationNames.GET_ITEM;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, ParsedDynamoDbRequest> parseRequest(DynamoDbQuery ddbQuery) {
        String tableName = readValidTableName(ddbQuery);
        QueryOperation keyCondition = parseKeyCondition(ddbQuery);
        return buildSingleTableRequestMap(tableName, keyCondition, null);
    }
}
