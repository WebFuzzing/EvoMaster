package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;

/**
 * Parser for DynamoDB {@code UpdateItem} requests.
 */
public class UpdateItemApiMethodParser extends WriteMethodParser {

    /**
     * {@inheritDoc}
     */
    @Override
    public DynamoDbOperationNames apiMethodName() {
        return DynamoDbOperationNames.UPDATE_ITEM;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean requiresKeyCondition() {
        return true;
    }
}
