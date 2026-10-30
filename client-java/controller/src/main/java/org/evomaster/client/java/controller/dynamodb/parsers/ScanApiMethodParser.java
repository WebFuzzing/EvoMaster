package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.controller.dynamodb.ParsedDynamoDbRequest;
import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbOperationNames;
import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbQuery;

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
    public Map<String, ParsedDynamoDbRequest> parseRequest(DynamoDbQuery ddbQuery) {
        String tableName = readValidTableName(ddbQuery);
        if (tableName == null) {
            return Collections.emptyMap();
        }

        QueryOperation filterExpression = parseExpression(
                ddbQuery.filterExpression(),
                readNameMap(ddbQuery),
                readValueMap(ddbQuery)
        );

        return buildSingleTableRequestMap(tableName, null, filterExpression);
    }
}
