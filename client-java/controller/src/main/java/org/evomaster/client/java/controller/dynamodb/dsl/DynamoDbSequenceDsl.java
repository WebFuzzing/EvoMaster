package org.evomaster.client.java.controller.dynamodb.dsl;

/**
 * Entry point for a DynamoDB insertion sequence.
 */
public interface DynamoDbSequenceDsl {

    /**
     * Starts an item insertion.
     *
     * @param tableName target table
     * @return item statement
     */
    DynamoDbStatementDsl insertInto(String tableName);
}
