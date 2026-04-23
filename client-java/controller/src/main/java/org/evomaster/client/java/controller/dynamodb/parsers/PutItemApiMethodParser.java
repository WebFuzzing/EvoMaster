package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;

public class PutItemApiMethodParser extends WriteMethodParser {

    @Override
    public DynamoDbOperationNames apiMethodName() {
        return DynamoDbOperationNames.PUT_ITEM;
    }

    @Override
    protected boolean requiresKeyCondition() {
        return false;
    }
}
