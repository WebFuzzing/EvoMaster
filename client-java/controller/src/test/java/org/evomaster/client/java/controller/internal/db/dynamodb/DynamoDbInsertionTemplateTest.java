package org.evomaster.client.java.controller.internal.db.dynamodb;

import org.evomaster.client.java.controller.api.dto.database.execution.DynamoDbFailedQuery;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbAttributeValueDto;
import org.evomaster.client.java.controller.api.dto.database.operations.DynamoDbScalarTypeDto;
import org.evomaster.client.java.controller.dynamodb.DynamoDbCommandExecutor;
import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbCommand;
import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbOperationNames;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.net.URI;
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Tests that inferred data supports subsequent reads of non-key application attributes. */
public class DynamoDbInsertionTemplateTest {

    private static final GenericContainer<?> dynamoDb = new GenericContainer<>("amazon/dynamodb-local:2.5.3")
            .withExposedPorts(8000)
            .withCommand("-jar", "DynamoDBLocal.jar", "-inMemory", "-sharedDb");
    private static DynamoDbClient client;

    @BeforeAll
    public static void setup() {
        dynamoDb.start();
        client = DynamoDbClient.builder()
                .endpointOverride(URI.create("http://" + dynamoDb.getHost() + ":" + dynamoDb.getMappedPort(8000)))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build();
        createTable("WorldCupRecords", "subject_id", "record_key");
        createTable("WorldCupPolicies", "purpose", null);

        Map<String, AttributeValue> record = new LinkedHashMap<>();
        record.put("subject_id", AttributeValue.builder().s("Diego Maradona").build());
        record.put("record_key", AttributeValue.builder().s("final-ticket").build());
        record.put("purpose", AttributeValue.builder().s("World Cup tickets").build());
        record.put("version", AttributeValue.builder().n("1").build());
        record.put("tombstoned", AttributeValue.builder().bool(false).build());
        client.putItem(PutItemRequest.builder().tableName("WorldCupRecords").item(record).build());
        Map<String, AttributeValue> policy = new LinkedHashMap<>();
        policy.put("purpose", record.get("purpose"));
        policy.put("retention_days", AttributeValue.builder().n("30").build());
        client.putItem(PutItemRequest.builder().tableName("WorldCupPolicies").item(policy).build());
    }

    @AfterAll
    public static void cleanup() {
        if (client != null) {
            client.close();
        }
        dynamoDb.stop();
    }

    @Test
    public void testInsertedRecordCanResolveItsPolicy() {
        QueryRequest query = QueryRequest.builder().tableName("WorldCupRecords")
                .keyConditionExpression("subject_id = :subject")
                .expressionAttributeValues(Collections.singletonMap(":subject",
                        AttributeValue.builder().s("Lionel Messi").build()))
                .consistentRead(true).build();
        assertEquals(0, client.query(query).count());
        DynamoDbHandler handler = new DynamoDbHandler();
        handler.setDynamoDbClient(client);
        handler.handle(new DynamoDbCommand(Collections.singletonList("WorldCupRecords"),
                DynamoDbOperationNames.QUERY, query, true, 1L));
        handler.getEvaluatedDynamoDbCommands();
        DynamoDbFailedQuery inferred = handler.getExecutionDto().failedQueries.get(0);
        DynamoDbInsertionDto insertion = new DynamoDbInsertionDto();
        insertion.tableName = inferred.tableName;
        assertEquals(Collections.singletonMap("record_key", DynamoDbScalarTypeDto.STRING),
                inferred.missingPrimaryKeyAttributes);
        // Core supplies missing keys; this controller test supplies one explicitly before executing the DTO.
        insertion.attributes = new ArrayList<>(inferred.attributes);
        insertion.attributes.add(new DynamoDbAttributeValueDto("record_key", DynamoDbScalarTypeDto.STRING,
                "messi-final-ticket"));

        assertEquals(Collections.singletonList(true), DynamoDbCommandExecutor.executeInsert(client,
                Collections.singletonList(insertion)).executionResults);
        Map<String, AttributeValue> record = client.query(query).items().get(0);
        Map<String, AttributeValue> policy = client.getItem(GetItemRequest.builder().tableName("WorldCupPolicies")
                .key(Collections.singletonMap("purpose", record.get("purpose"))).consistentRead(true).build()).item();

        assertEquals("Lionel Messi", record.get("subject_id").s());
        assertFalse("final-ticket".equals(record.get("record_key").s()));
        assertEquals("30", policy.get("retention_days").n());
        assertEquals(2L, Long.parseLong(record.get("version").n()) + 1);
        assertFalse(record.get("tombstoned").bool());
    }

    private static void createTable(String tableName, String hashKey, String sortKey) {
        CreateTableRequest.Builder table = CreateTableRequest.builder().tableName(tableName)
                .billingMode(BillingMode.PAY_PER_REQUEST);
        AttributeDefinition hashDefinition = AttributeDefinition.builder().attributeName(hashKey)
                .attributeType(ScalarAttributeType.S).build();
        KeySchemaElement hashSchema = KeySchemaElement.builder().attributeName(hashKey).keyType(KeyType.HASH).build();
        if (sortKey == null) {
            table.attributeDefinitions(hashDefinition).keySchema(hashSchema);
        } else {
            table.attributeDefinitions(hashDefinition, AttributeDefinition.builder().attributeName(sortKey)
                    .attributeType(ScalarAttributeType.S).build());
            table.keySchema(hashSchema, KeySchemaElement.builder().attributeName(sortKey).keyType(KeyType.RANGE).build());
        }
        client.createTable(table.build());
    }
}
