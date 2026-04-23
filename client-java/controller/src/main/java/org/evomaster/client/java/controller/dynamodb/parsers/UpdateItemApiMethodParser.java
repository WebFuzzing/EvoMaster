package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;

public class UpdateItemApiMethodParser extends WriteMethodParser {

    @Override
    public DynamoDbOperationNames apiMethodName() {
        return DynamoDbOperationNames.UPDATE_ITEM;
    }

    @Override
    protected boolean requiresKeyCondition() {
        return true;
    }
}
