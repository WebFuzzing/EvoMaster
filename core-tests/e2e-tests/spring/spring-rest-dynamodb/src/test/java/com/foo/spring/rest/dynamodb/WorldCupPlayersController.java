package com.foo.spring.rest.dynamodb;

import com.dynamodb.players.WorldCupPlayersApp;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.util.HashMap;
import java.util.Map;

/**
 * EvoMaster embedded controller for the World Cup players DynamoDB application.
 */
public class WorldCupPlayersController extends DynamoDbController {

    private static final String TABLE_NAME = "WorldCupPlayers";

    /**
     * Returns the World Cup players Spring application.
     *
     * @return application class
     */
    @Override
    protected Class<?> getApplicationClass() {
        return WorldCupPlayersApp.class;
    }

    /**
     * Creates and seeds the World Cup players table.
     *
     * @param client DynamoDB Local administration client
     */
    @Override
    protected void initializeDatabase(DynamoDbAsyncClient client) {
        CreateTableRequest createTable = CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName("country")
                        .attributeType(ScalarAttributeType.S)
                        .build())
                .keySchema(KeySchemaElement.builder()
                        .attributeName("country")
                        .keyType(KeyType.HASH)
                        .build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(1L)
                        .writeCapacityUnits(1L)
                        .build())
                .build();
        client.createTable(createTable).join();

        Map<String, AttributeValue> lionelMessi = new HashMap<>();
        lionelMessi.put("country", AttributeValue.builder().s("Argentina").build());
        lionelMessi.put("playerName", AttributeValue.builder().s("Lionel Messi").build());
        lionelMessi.put("fifaId", AttributeValue.builder().n("158023").build());
        lionelMessi.put("shirtNumber", AttributeValue.builder().n("10").build());
        client.putItem(PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(lionelMessi)
                .build()).join();
    }

    /**
     * Selects the World Cup players package for instrumentation.
     *
     * @return package prefix to cover
     */
    @Override
    public String getPackagePrefixesToCover() {
        return "com.dynamodb.players";
    }
}
