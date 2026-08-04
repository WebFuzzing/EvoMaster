package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.controller.dynamodb.ParsedDynamoDbRequest;
import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;

import java.util.Collections;
import java.util.Map;

/**
 * Parser for DynamoDB {@code Scan} requests.
 */
public class ScanApiMethodParser extends DynamoDbBaseApiMethodParser {

    /**
     * {@inheritDoc}
     */
    @Override
    public DynamoDbOperationNames apiMethodName() {
        return DynamoDbOperationNames.SCAN;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, ParsedDynamoDbRequest> parseRequest(Object ddbRequest) {
        String tableName = readValidTableName(ddbRequest);
        if (tableName == null) {
            return Collections.emptyMap();
        }

        QueryOperation filterExpression = parseExpression(
                readString(ddbRequest, METHOD_FILTER_EXPRESSION),
                readNameMap(ddbRequest),
                readValueMap(ddbRequest)
        );

        return buildSingleTableRequestMap(tableName, null, filterExpression);
    }
}
