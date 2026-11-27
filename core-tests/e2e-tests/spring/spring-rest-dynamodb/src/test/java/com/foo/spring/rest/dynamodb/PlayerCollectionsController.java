package com.foo.spring.rest.dynamodb;

import com.dynamodb.collections.PlayerCollectionsApp;
import com.dynamodb.collections.PlayerCollectionsRest;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * EvoMaster embedded controller for the DynamoDB collections/documents/sets/nulls E2E application.
 */
public class PlayerCollectionsController extends DynamoDbController {

    @Override
    protected Class<?> getApplicationClass() {
        return PlayerCollectionsApp.class;
    }

    @Override
    protected void initializeDatabase(DynamoDbAsyncClient client) {
        CreateTableRequest createTable = CreateTableRequest.builder()
                .tableName(PlayerCollectionsRest.TABLE_NAME)
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

        seedItem(client);
    }

    @Override
    protected void resetDatabase(DynamoDbAsyncClient client) {
        ScanResponse scan = client.scan(ScanRequest.builder()
                .tableName(PlayerCollectionsRest.TABLE_NAME)
                .consistentRead(true)
                .build()).join();
        for (Map<String, AttributeValue> item : scan.items()) {
            AttributeValue key = item.get("country");
            if (key != null && !PlayerCollectionsRest.SEED_COUNTRY.equals(key.s())) {
                client.deleteItem(DeleteItemRequest.builder()
                        .tableName(PlayerCollectionsRest.TABLE_NAME)
                        .key(Collections.singletonMap("country", key))
                        .build()).join();
            }
        }
    }

    @Override
    public String getPackagePrefixesToCover() {
        return "com.dynamodb.collections";
    }

    private static void seedItem(DynamoDbAsyncClient client) {
        Map<String, AttributeValue> clubMap = new LinkedHashMap<>();
        clubMap.put("city", AttributeValue.builder().s("Miami").build());

        Map<String, AttributeValue> profileMap = new LinkedHashMap<>();
        profileMap.put("captain", AttributeValue.builder().s("Lionel Messi").build());
        profileMap.put("caps", AttributeValue.builder().n("180").build());
        profileMap.put("verified", AttributeValue.builder().bool(true).build());
        profileMap.put("club", AttributeValue.builder().m(clubMap).build());

        Map<String, AttributeValue> nestedListMap = new LinkedHashMap<>();
        nestedListMap.put("tier", AttributeValue.builder().s("gold").build());

        AttributeValue tags = AttributeValue.builder().l(Arrays.asList(
                AttributeValue.builder().s("world-champion").build(),
                AttributeValue.builder().m(nestedListMap).build(),
                AttributeValue.builder().l(Collections.singletonList(
                        AttributeValue.builder().s("nested-list").build()
                )).build(),
                AttributeValue.builder().ss("left", "right").build(),
                AttributeValue.builder().ns("1", "2").build(),
                AttributeValue.builder().nul(true).build()
        )).build();

        Map<String, AttributeValue> item = new LinkedHashMap<>();
        item.put("country", AttributeValue.builder().s(PlayerCollectionsRest.SEED_COUNTRY).build());
        item.put("profile", AttributeValue.builder().m(profileMap).build());
        item.put("tags", tags);
        item.put("roles", AttributeValue.builder().ss("captain", "forward").build());
        item.put("jerseyNumbers", AttributeValue.builder().ns("10", "30").build());
        item.put("retiredAt", AttributeValue.builder().nul(true).build());
        item.put("emptyTags", AttributeValue.builder().l(Collections.emptyList()).build());

        client.putItem(PutItemRequest.builder()
                .tableName(PlayerCollectionsRest.TABLE_NAME)
                .item(item)
                .build()).join();
    }
}
