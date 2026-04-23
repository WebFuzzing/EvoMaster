package org.evomaster.client.java.controller.dynamodb.parsers;

import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;

/**
 * Parser for DynamoDB {@code PutItem} requests.
 */
public class PutItemApiMethodParser extends WriteMethodParser {

    /**
     * {@inheritDoc}
     */
    @Override
    public DynamoDbOperationNames apiMethodName() {
        return DynamoDbOperationNames.PUT_ITEM;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean requiresKeyCondition() {
        return false;
    }
}
