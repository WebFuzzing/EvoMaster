package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.controller.dynamodb.operations.QueryOperation;
import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;

import java.util.Map;

public class GetItemApiMethodParser extends DynamoDbBaseApiMethodParser {

    @Override
    public DynamoDbOperationNames apiMethodName() {
        return DynamoDbOperationNames.GET_ITEM;
    }

    @Override
    public Map<String, QueryOperation> parseRequest(Object request) {
        String tableName = readValidTableName(request);
        QueryOperation keyCondition = parseKeyCondition(request);
        return singleTableResult(tableName, keyCondition);
    }
}
