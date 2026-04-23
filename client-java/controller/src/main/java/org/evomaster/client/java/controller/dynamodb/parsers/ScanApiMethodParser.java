package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;

import java.util.Collections;
import java.util.Map;

public class ScanApiMethodParser extends DynamoDbBaseApiMethodParser {

    @Override
    public DynamoDbOperationNames apiMethodName() {
        return DynamoDbOperationNames.SCAN;
    }

    @Override
    public Map<String, QueryOperation> parseRequest(Object request) {
        String tableName = readValidTableName(request);
        if (tableName == null) {
            return Collections.emptyMap();
        }

        QueryOperation filterCondition = parseExpression(
                readString(request, METHOD_FILTER_EXPRESSION),
                readNameMap(request),
                readValueMap(request)
        );

        return singleTableResult(tableName, filterCondition);
    }
}
