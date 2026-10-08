package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.DynamoDbCommand;
import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.Protocol;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DynamoDbClassReplacementTest {

    private static final int DYNAMODB_PORT = 28001;
    @SuppressWarnings("resource")
    private static final GenericContainer<?> dynamoDb = new GenericContainer<>("amazon/dynamodb-local:latest")
            .withExposedPorts(DYNAMODB_PORT)
            .withCommand("-jar", "DynamoDBLocal.jar", "-inMemory", "-port", String.valueOf(DYNAMODB_PORT))
            .waitingFor(Wait.forListeningPort());
    private static DynamoDbClient syncClient;
    private static DynamoDbAsyncClient asyncClient;
    private static final String TABLE_NAME = "evomasterDDBTestTableA";
    private static final String TABLE_NAME_SECOND = "evomasterDDBTestTableB";

    @BeforeAll
    public static void setup() {
        dynamoDb.start();
        int port = dynamoDb.getMappedPort(DYNAMODB_PORT);
        String endpoint = "http://localhost:" + port;
        URI endpointUri = URI.create(endpoint);

        syncClient = DynamoDbClient.builder()
                .endpointOverride(endpointUri)
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("dummy", "dummy")))
                .build();

        asyncClient = DynamoDbAsyncClient.builder()
                .endpointOverride(endpointUri)
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("dummy", "dummy")))
                .httpClientBuilder(NettyNioAsyncHttpClient.builder()
                        .protocol(Protocol.HTTP1_1))
                .build();

        createTable(TABLE_NAME);
        createTable(TABLE_NAME_SECOND);
    }

    private static void createTable(String tableName) {
        syncClient.createTable(CreateTableRequest.builder()
                .tableName(tableName)
                .keySchema(KeySchemaElement.builder()
                        .attributeName("id")
                        .keyType(KeyType.HASH)
                        .build())
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName("id")
                        .attributeType(ScalarAttributeType.S)
                        .build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(5L)
                        .writeCapacityUnits(5L)
                        .build())
                .build());
    }

    @AfterAll
    public static void teardown() {
        if (syncClient != null) {
            syncClient.close();
        }
        if (asyncClient != null) {
            asyncClient.close();
        }
        if (dynamoDb.isRunning()) {
            dynamoDb.stop();
        }
    }

    @BeforeEach
    public void resetExecutionTracer() {
        ExecutionTracer.reset();
    }

    // --- Sync Tests ---

    @Test
    public void testGetItem() {
        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Collections.singletonMap("id", AttributeValue.builder().s("1").build()))
                .build();

        GetItemResponse result = (GetItemResponse) DynamoDbClassReplacement.Sync.getItem(syncClient, request);

        assertNotNull(result);
        verifyInterception(Collections.singletonList(TABLE_NAME), DynamoDbOperationNames.GET_ITEM, request);
    }

    @Test
    public void testPutItem() {
        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(Collections.singletonMap("id", AttributeValue.builder().s("1").build()))
                .build();

        PutItemResponse result = (PutItemResponse) DynamoDbClassReplacement.Sync.putItem(syncClient, request);

        assertNotNull(result);
        verifyInterception(Collections.singletonList(TABLE_NAME), DynamoDbOperationNames.PUT_ITEM, request);
    }

    @Test
    public void testUpdateItem() {
        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Collections.singletonMap("id", AttributeValue.builder().s("1").build()))
                .updateExpression("SET #v = :value")
                .expressionAttributeNames(Collections.singletonMap("#v", "value"))
                .expressionAttributeValues(Collections.singletonMap(":value", AttributeValue.builder().s("updated").build()))
                .build();

        UpdateItemResponse result = (UpdateItemResponse) DynamoDbClassReplacement.Sync.updateItem(syncClient, request);

        assertNotNull(result);
        verifyInterception(Collections.singletonList(TABLE_NAME), DynamoDbOperationNames.UPDATE_ITEM, request);
    }

    @Test
    public void testDeleteItem() {
        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Collections.singletonMap("id", AttributeValue.builder().s("1").build()))
                .build();

        DeleteItemResponse result = (DeleteItemResponse) DynamoDbClassReplacement.Sync.deleteItem(syncClient, request);

        assertNotNull(result);
        verifyInterception(Collections.singletonList(TABLE_NAME), DynamoDbOperationNames.DELETE_ITEM, request);
    }

    @Test
    public void testQuery() {
        QueryRequest request = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .keyConditionExpression("id = :v_id")
                .expressionAttributeValues(Collections.singletonMap(":v_id", AttributeValue.builder().s("1").build()))
                .build();

        QueryResponse result = (QueryResponse) DynamoDbClassReplacement.Sync.query(syncClient, request);

        assertNotNull(result);
        verifyInterception(Collections.singletonList(TABLE_NAME), DynamoDbOperationNames.QUERY, request);
    }

    @Test
    public void testScan() {
        ScanRequest request = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .build();

        ScanResponse result = (ScanResponse) DynamoDbClassReplacement.Sync.scan(syncClient, request);

        assertNotNull(result);
        verifyInterception(Collections.singletonList(TABLE_NAME), DynamoDbOperationNames.SCAN, request);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBatchGetItem() {
        KeysAndAttributes keysAndAttributes = KeysAndAttributes.builder()
                .keys(Collections.singletonMap("id", AttributeValue.builder().s("1").build()))
                .build();
        Map<String, KeysAndAttributes> requestItems = new HashMap<>();
        requestItems.put(TABLE_NAME, keysAndAttributes);
        requestItems.put(TABLE_NAME_SECOND, keysAndAttributes);
        BatchGetItemRequest request = BatchGetItemRequest.builder()
                .requestItems(requestItems)
                .build();

        BatchGetItemResponse result = (BatchGetItemResponse) DynamoDbClassReplacement.Sync.batchGetItem(syncClient, request);

        assertNotNull(result);
        verifyInterception(Arrays.asList(TABLE_NAME, TABLE_NAME_SECOND), DynamoDbOperationNames.BATCH_GET_ITEM, request);
    }

    // --- Async Tests ---

    @Test
    @SuppressWarnings("unchecked")
    public void testGetItemAsync() {
        GetItemRequest request = GetItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Collections.singletonMap("id", AttributeValue.builder().s("1").build()))
                .build();

        CompletableFuture<GetItemResponse> resultFuture = (CompletableFuture<GetItemResponse>) DynamoDbClassReplacement.Async.getItem(asyncClient, request);

        try {
            resultFuture.get();
        } catch (Exception e) {
            // ignore
        }
        verifyInterception(Collections.singletonList(TABLE_NAME), DynamoDbOperationNames.GET_ITEM, request);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPutItemAsync() {
        PutItemRequest request = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(Collections.singletonMap("id", AttributeValue.builder().s("2").build()))
                .build();

        CompletableFuture<PutItemResponse> resultFuture = (CompletableFuture<PutItemResponse>) DynamoDbClassReplacement.Async.putItem(asyncClient, request);

        try {
            resultFuture.get();
        } catch (Exception e) {
            // ignore
        }
        verifyInterception(Collections.singletonList(TABLE_NAME), DynamoDbOperationNames.PUT_ITEM, request);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateItemAsync() {
        UpdateItemRequest request = UpdateItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Collections.singletonMap("id", AttributeValue.builder().s("2").build()))
                .updateExpression("SET #v = :value")
                .expressionAttributeNames(Collections.singletonMap("#v", "value"))
                .expressionAttributeValues(Collections.singletonMap(":value", AttributeValue.builder().s("updatedAsync").build()))
                .build();

        CompletableFuture<UpdateItemResponse> resultFuture = (CompletableFuture<UpdateItemResponse>) DynamoDbClassReplacement.Async.updateItem(asyncClient, request);

        try {
            resultFuture.get();
        } catch (Exception e) {
            // ignore
        }
        verifyInterception(Collections.singletonList(TABLE_NAME), DynamoDbOperationNames.UPDATE_ITEM, request);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDeleteItemAsync() {
        DeleteItemRequest request = DeleteItemRequest.builder()
                .tableName(TABLE_NAME)
                .key(Collections.singletonMap("id", AttributeValue.builder().s("2").build()))
                .build();

        CompletableFuture<DeleteItemResponse> resultFuture = (CompletableFuture<DeleteItemResponse>) DynamoDbClassReplacement.Async.deleteItem(asyncClient, request);

        try {
            resultFuture.get();
        } catch (Exception e) {
            // ignore
        }
        verifyInterception(Collections.singletonList(TABLE_NAME), DynamoDbOperationNames.DELETE_ITEM, request);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testQueryAsync() {
        QueryRequest request = QueryRequest.builder()
                .tableName(TABLE_NAME)
                .keyConditionExpression("id = :v_id")
                .expressionAttributeValues(Collections.singletonMap(":v_id", AttributeValue.builder().s("1").build()))
                .build();

        CompletableFuture<QueryResponse> resultFuture = (CompletableFuture<QueryResponse>) DynamoDbClassReplacement.Async.query(asyncClient, request);

        try {
            resultFuture.get();
        } catch (Exception e) {
            // ignore
        }
        verifyInterception(Collections.singletonList(TABLE_NAME), DynamoDbOperationNames.QUERY, request);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testScanAsync() {
        ScanRequest request = ScanRequest.builder()
                .tableName(TABLE_NAME)
                .build();

        CompletableFuture<ScanResponse> resultFuture = (CompletableFuture<ScanResponse>) DynamoDbClassReplacement.Async.scan(asyncClient, request);

        try {
            resultFuture.get();
        } catch (Exception e) {
            // ignore
        }
        verifyInterception(Collections.singletonList(TABLE_NAME), DynamoDbOperationNames.SCAN, request);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBatchGetItemAsync() {
        KeysAndAttributes keysAndAttributes = KeysAndAttributes.builder()
                .keys(Collections.singletonMap("id", AttributeValue.builder().s("1").build()))
                .build();
        Map<String, KeysAndAttributes> requestItems = new HashMap<>();
        requestItems.put(TABLE_NAME, keysAndAttributes);
        requestItems.put(TABLE_NAME_SECOND, keysAndAttributes);
        BatchGetItemRequest request = BatchGetItemRequest.builder()
                .requestItems(requestItems)
                .build();

        CompletableFuture<BatchGetItemResponse> resultFuture = (CompletableFuture<BatchGetItemResponse>) DynamoDbClassReplacement.Async.batchGetItem(asyncClient, request);

        try {
            resultFuture.get();
        } catch (Exception e) {
            // ignore
        }
        verifyInterception(Arrays.asList(TABLE_NAME, TABLE_NAME_SECOND), DynamoDbOperationNames.BATCH_GET_ITEM, request);
    }

    private void verifyInterception(List<String> expectedTableNames, DynamoDbOperationNames expectedOperationName, Object expectedRequest) {
        List<AdditionalInfo> additionalInfoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, additionalInfoList.size());
        Set<DynamoDbCommand> dynamoDbCommands = additionalInfoList.get(0).getDynamoDbInfoData();
        assertEquals(1, dynamoDbCommands.size());

        DynamoDbCommand command = dynamoDbCommands.iterator().next();
        assertEquals(expectedTableNames, command.getTableNames());
        assertEquals(expectedOperationName, command.getOperationName());
        assertEquals(expectedRequest, command.getDdbRequest());
    }
}
