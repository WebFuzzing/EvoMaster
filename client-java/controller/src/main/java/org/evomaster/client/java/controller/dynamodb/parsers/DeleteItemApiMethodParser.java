package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;

/**
 * Parser for DynamoDB {@code DeleteItem} requests.
 */
public class DeleteItemApiMethodParser extends WriteMethodParser {

    /**
     * {@inheritDoc}
     */
    @Override
    public DynamoDbOperationNames apiMethodName() {
        return DynamoDbOperationNames.DELETE_ITEM;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean requiresKeyCondition() {
        return true;
    }
}
