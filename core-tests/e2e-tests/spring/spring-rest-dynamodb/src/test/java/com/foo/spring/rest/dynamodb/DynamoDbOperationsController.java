package com.foo.spring.rest.dynamodb;

import com.dynamodb.operations.DynamoDbOperationsApp;
import com.dynamodb.operations.DynamoDbOperationsData;
import com.dynamodb.operations.DynamoDbOperationsData.ClientMode;
import com.dynamodb.operations.DynamoDbOperationsData.Operation;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * EvoMaster embedded controller for the DynamoDB operation-matrix application.
 */
public class DynamoDbOperationsController extends DynamoDbController {

    /**
     * Returns the DynamoDB operation-matrix Spring application.
     *
     * @return application class
     */
    @Override
    protected Class<?> getApplicationClass() {
        return DynamoDbOperationsApp.class;
    }

    /**
     * Creates the operation table and seeds every client-operation scenario.
     *
     * @param client DynamoDB Local administration client
     */
    @Override
    protected void initializeDatabase(DynamoDbAsyncClient client) {
        CreateTableRequest createTable = CreateTableRequest.builder()
                .tableName(DynamoDbOperationsData.TABLE_NAME)
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName(DynamoDbOperationsData.SCENARIO_ATTRIBUTE)
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName(DynamoDbOperationsData.SHIRT_NUMBER_ATTRIBUTE)
                                .attributeType(ScalarAttributeType.N)
                                .build())
                .keySchema(
                        KeySchemaElement.builder()
                                .attributeName(DynamoDbOperationsData.SCENARIO_ATTRIBUTE)
                                .keyType(KeyType.HASH)
                                .build(),
                        KeySchemaElement.builder()
                                .attributeName(DynamoDbOperationsData.SHIRT_NUMBER_ATTRIBUTE)
                                .keyType(KeyType.RANGE)
                                .build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(1L)
                        .writeCapacityUnits(1L)
                        .build())
                .build();
        client.createTable(createTable).join();
        seedOperationItems(client);
    }

    /**
     * Selects the DynamoDB operations package for instrumentation.
     *
     * @return package prefix to cover
     */
    @Override
    public String getPackagePrefixesToCover() {
        return "com.dynamodb.operations";
    }

    /**
     * Writes the canonical item for all fourteen matrix scenarios during startup.
     *
     * @param client DynamoDB Local administration client
     */
    private void seedOperationItems(DynamoDbAsyncClient client) {
        for (ClientMode clientMode : ClientMode.values()) {
            for (Operation operation : Operation.values()) {
                client.putItem(PutItemRequest.builder()
                        .tableName(DynamoDbOperationsData.TABLE_NAME)
                        .item(DynamoDbOperationsData.item(clientMode, operation))
                        .build()).join();
            }
        }
    }
}
