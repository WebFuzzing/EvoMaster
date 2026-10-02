package com.foo.spring.rest.dynamodb;

import com.dynamodb.players.WorldCupPlayersApp;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/** Embedded controller for an empty World Cup players DynamoDB table. */
public class WorldCupPlayersEmptyController extends DynamoDbController {

    /** Creates the table used by the read-only player endpoint without seed items. */
    @Override
    protected void initializeDatabase(DynamoDbAsyncClient client) {
        client.createTable(CreateTableRequest.builder()
                .tableName("WorldCupPlayers")
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName("country")
                        .attributeType(ScalarAttributeType.S)
                        .build())
                .keySchema(KeySchemaElement.builder().attributeName("country").keyType(KeyType.HASH).build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(1L).writeCapacityUnits(1L).build())
                .build()).join();
    }

    @Override
    protected Class<?> getApplicationClass() {
        return WorldCupPlayersApp.class;
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.dynamodb.players";
    }
}
