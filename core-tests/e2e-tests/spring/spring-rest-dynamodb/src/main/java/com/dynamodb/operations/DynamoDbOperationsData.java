package com.dynamodb.operations;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

/**
 * Shared table schema and deterministic World Cup fixture for DynamoDB operation tests.
 */
public final class DynamoDbOperationsData {

    public static final String TABLE_NAME = "DynamoDbOperations";
    public static final String SCENARIO_ATTRIBUTE = "scenario";
    public static final String SHIRT_NUMBER_ATTRIBUTE = "shirtNumber";
    public static final String PLAYER_NAME_ATTRIBUTE = "playerName";
    public static final int MISSING_SHIRT_NUMBER = 9;
    public static final int TARGET_SHIRT_NUMBER = 10;

    private DynamoDbOperationsData() {
    }

    /**
     * DynamoDB SDK client variants exercised by the SUT.
     */
    public enum ClientMode {
        SYNC,
        ASYNC
    }

    /**
     * DynamoDB operations exercised by the SUT.
     */
    public enum Operation {
        GET_ITEM,
        BATCH_GET_ITEM,
        PUT_ITEM,
        UPDATE_ITEM,
        DELETE_ITEM,
        QUERY,
        SCAN
    }

    /**
     * Builds the partition-key value for one client and operation pair.
     *
     * @param clientMode SDK client variant
     * @param operation DynamoDB operation
     * @return deterministic scenario name
     */
    public static String scenario(ClientMode clientMode, Operation operation) {
        return clientMode.name() + "_" + operation.name();
    }

    /**
     * Builds a DynamoDB key for one operation scenario and shirt number.
     *
     * @param clientMode SDK client variant
     * @param operation DynamoDB operation
     * @param shirtNumber requested shirt number
     * @return DynamoDB key attributes
     */
    public static Map<String, AttributeValue> key(
            ClientMode clientMode, Operation operation, int shirtNumber) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put(SCENARIO_ATTRIBUTE, AttributeValue.builder().s(scenario(clientMode, operation)).build());
        key.put(SHIRT_NUMBER_ATTRIBUTE, AttributeValue.builder().n(Integer.toString(shirtNumber)).build());
        return key;
    }

    /**
     * Builds the canonical Lionel Messi item for one operation scenario.
     *
     * @param clientMode SDK client variant
     * @param operation DynamoDB operation
     * @return complete DynamoDB item
     */
    public static Map<String, AttributeValue> item(ClientMode clientMode, Operation operation) {
        Map<String, AttributeValue> item = key(clientMode, operation, TARGET_SHIRT_NUMBER);
        item.put(PLAYER_NAME_ATTRIBUTE, AttributeValue.builder().s("Lionel Messi").build());
        item.put("country", AttributeValue.builder().s("Argentina").build());
        return item;
    }
}
