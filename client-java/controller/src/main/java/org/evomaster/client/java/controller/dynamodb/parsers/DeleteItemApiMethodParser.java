package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;

public class DeleteItemApiMethodParser extends WriteMethodParser {

    @Override
    public DynamoDbOperationNames apiMethodName() {
        return DynamoDbOperationNames.DELETE_ITEM;
    }

    @Override
    protected boolean requiresKeyCondition() {
        return true;
    }
}
