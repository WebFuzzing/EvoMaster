package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.controller.dynamodb.ParsedDynamoDbRequest;
import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.instrumentation.DynamoDbQuery;

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
    public final Map<String, ParsedDynamoDbRequest> parseRequest(DynamoDbQuery ddbQuery) {
        String tableName = readValidTableName(ddbQuery);
        if (tableName == null) {
            return Collections.emptyMap();
        }

        QueryOperation keyCondition = requiresKeyCondition() ? parseKeyCondition(ddbQuery) : null;
        QueryOperation filterExpression = parseExpression(
                ddbQuery.filterExpression(),
                readNameMap(ddbQuery),
                readValueMap(ddbQuery)
        );

        return buildSingleTableRequestMap(tableName, keyCondition, filterExpression);
    }
}
