package org.evomaster.client.java.instrumentation;

/**
 * Shared DynamoDB API operation names used across instrumentation and parsing logic.
 */
public enum DynamoDbOperationNames {

    GET_ITEM("GetItem"),
    BATCH_GET_ITEM("BatchGetItem"),
    PUT_ITEM("PutItem"),
    UPDATE_ITEM("UpdateItem"),
    DELETE_ITEM("DeleteItem"),
    QUERY("Query"),
    SCAN("Scan");

    private final String value;

    DynamoDbOperationNames(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
