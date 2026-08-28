package org.evomaster.client.java.instrumentation.example.dynamodb;

import com.foo.somedifferentpackage.examples.dynamodb.EnhancedDynamoDbOperationsImpl;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.DynamoDbCommand;
import org.evomaster.client.java.instrumentation.DynamoDbOperationNames;
import org.evomaster.client.java.instrumentation.InputProperties;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testcontainers-backed interception tests for instrumented DynamoDB enhanced clients.
 */
public class DynamoDbEnhancedClientInstrumentedTest {

    private static final int DYNAMODB_PORT = 28003;
    private static final String TABLE_NAME = "WorldCupPlayers";
    private static final String MODEL_PACKAGE = "software.amazon.awssdk.services.dynamodb.model.";
    private static final GenericContainer<?> DYNAMODB = new GenericContainer<>("amazon/dynamodb-local:latest")
            .withExposedPorts(DYNAMODB_PORT)
            .withCommand("-jar", "DynamoDBLocal.jar", "-inMemory", "-port", String.valueOf(DYNAMODB_PORT))
            .waitingFor(Wait.forListeningPort());

    private static String previousCategories;
    private static DynamoDbClient adminClient;
    private static EnhancedDynamoDbOperations operations;

    /**
     * Starts DynamoDB Local, seeds World Cup players, and loads the enhanced-client fixture through instrumentation.
     *
     * @throws Exception if the fixture cannot be loaded
     */
    @BeforeAll
    public static void setup() throws Exception {
        DYNAMODB.start();
        String endpoint = "http://localhost:" + DYNAMODB.getMappedPort(DYNAMODB_PORT);
        adminClient = DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("dummy", "dummy")))
                .build();
        createTable();
        seedPlayers();

        previousCategories = System.getProperty(InputProperties.REPLACEMENT_CATEGORIES);
        System.setProperty(InputProperties.REPLACEMENT_CATEGORIES,
                previousCategories == null ? "BASE,DYNAMODB" : previousCategories + ",DYNAMODB");
        InstrumentingClassLoader classLoader = new InstrumentingClassLoader("com.foo");
        classLoader.setCrashWhenFailedInstrumentation(true);
        operations = (EnhancedDynamoDbOperations) classLoader
                .loadClass(EnhancedDynamoDbOperationsImpl.class.getName())
                .getConstructor(String.class, String.class)
                .newInstance(endpoint, TABLE_NAME);
    }

    /**
     * Closes clients, restores configuration, and stops DynamoDB Local.
     */
    @AfterAll
    public static void teardown() {
        if (operations != null) {
            operations.close();
        }
        if (adminClient != null) {
            adminClient.close();
        }
        if (previousCategories == null) {
            System.clearProperty(InputProperties.REPLACEMENT_CATEGORIES);
        } else {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, previousCategories);
        }
        if (DYNAMODB.isRunning()) {
            DYNAMODB.stop();
        }
    }

    /**
     * Clears previously traced commands before each test.
     */
    @BeforeEach
    public void resetTracer() {
        ExecutionTracer.reset();
    }

    /**
     * Exercises all seven supported operations through synchronous and asynchronous enhanced clients.
     */
    @Test
    public void shouldRecordEnhancedOperationsThroughGeneratedLowLevelRequests() {
        List<OperationCase> cases = Arrays.asList(
                operation("sync-get", DynamoDbOperationNames.GET_ITEM, "GetItemRequest"),
                operation("async-get", DynamoDbOperationNames.GET_ITEM, "GetItemRequest"),
                operation("sync-batch-get", DynamoDbOperationNames.BATCH_GET_ITEM, "BatchGetItemRequest"),
                operation("async-batch-get", DynamoDbOperationNames.BATCH_GET_ITEM, "BatchGetItemRequest"),
                operation("sync-put", DynamoDbOperationNames.PUT_ITEM, "PutItemRequest"),
                operation("async-put", DynamoDbOperationNames.PUT_ITEM, "PutItemRequest"),
                operation("sync-update", DynamoDbOperationNames.UPDATE_ITEM, "UpdateItemRequest"),
                operation("async-update", DynamoDbOperationNames.UPDATE_ITEM, "UpdateItemRequest"),
                operation("sync-delete", DynamoDbOperationNames.DELETE_ITEM, "DeleteItemRequest"),
                operation("async-delete", DynamoDbOperationNames.DELETE_ITEM, "DeleteItemRequest"),
                operation("sync-query", DynamoDbOperationNames.QUERY, "QueryRequest"),
                operation("async-query", DynamoDbOperationNames.QUERY, "QueryRequest"),
                operation("sync-scan", DynamoDbOperationNames.SCAN, "ScanRequest"),
                operation("async-scan", DynamoDbOperationNames.SCAN, "ScanRequest"));

        for (OperationCase operationCase : cases) {
            ExecutionTracer.reset();
            int actualPages = operations.execute(operationCase.name);

            assertTrue(actualPages > 0, operationCase.name + " should consume at least one page");
            verifyCommands(operationCase, actualPages, true);
        }
    }

    /**
     * Verifies that synchronous and asynchronous conditional failures are recorded without changing their causes.
     */
    @Test
    public void shouldRecordConditionalFailuresAndPreserveObservedExceptions() {
        for (boolean async : Arrays.asList(false, true)) {
            ExecutionTracer.reset();

            RuntimeException failure = assertThrows(RuntimeException.class,
                    () -> operations.executeConditionalFailure(async));

            assertEquals(MODEL_PACKAGE + "ConditionalCheckFailedException", deepestCause(failure).getClass().getName());
            verifyCommands(operation(async ? "async-failed-put" : "sync-failed-put",
                    DynamoDbOperationNames.PUT_ITEM, "PutItemRequest"), 1, false);
        }
    }

    /**
     * Creates the composite-key player table.
     */
    private static void createTable() {
        adminClient.createTable(CreateTableRequest.builder()
                .tableName(TABLE_NAME)
                .keySchema(
                        KeySchemaElement.builder().attributeName("country").keyType(KeyType.HASH).build(),
                        KeySchemaElement.builder().attributeName("name").keyType(KeyType.RANGE).build())
                .attributeDefinitions(
                        AttributeDefinition.builder().attributeName("country").attributeType(ScalarAttributeType.S).build(),
                        AttributeDefinition.builder().attributeName("name").attributeType(ScalarAttributeType.S).build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(5L)
                        .writeCapacityUnits(5L)
                        .build())
                .build());
    }

    /**
     * Seeds deterministic World Cup player data.
     */
    private static void seedPlayers() {
        putPlayer("Argentina", "Lionel Messi", 36);
        putPlayer("Argentina", "Angel Di Maria", 36);
        putPlayer("France", "Kylian Mbappe", 25);
        putPlayer("France", "Antoine Griezmann", 33);
        putPlayer("Portugal", "Cristiano Ronaldo", 39);
        putPlayer("Brazil", "Vinicius Junior", 23);
        putPlayer("Spain", "Pedri Gonzalez", 21);
    }

    /**
     * Inserts one player through the uninstrumented administrative client.
     *
     * @param country player's country
     * @param name player's name
     * @param age player's age
     */
    private static void putPlayer(String country, String name, int age) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("country", AttributeValue.builder().s(country).build());
        item.put("name", AttributeValue.builder().s(name).build());
        item.put("age", AttributeValue.builder().n(String.valueOf(age)).build());
        adminClient.putItem(PutItemRequest.builder().tableName(TABLE_NAME).item(item).build());
    }

    /**
     * Verifies all commands recorded for one enhanced operation.
     *
     * @param operationCase expected operation details
     * @param expectedCount expected low-level page request count
     * @param expectedSuccess expected outcome
     */
    private void verifyCommands(OperationCase operationCase, int expectedCount, boolean expectedSuccess) {
        List<DynamoDbCommand> commands = recordedCommands();
        assertEquals(expectedCount, commands.size(), operationCase.name);
        for (DynamoDbCommand command : commands) {
            assertEquals(Collections.singletonList(TABLE_NAME), command.getTableNames(), operationCase.name);
            assertEquals(operationCase.operationName, command.getOperationName(), operationCase.name);
            assertEquals(MODEL_PACKAGE + operationCase.requestSimpleName,
                    command.getDdbRequest().getClass().getName(), operationCase.name);
            assertEquals(expectedSuccess, command.isSuccessfullyExecuted(), operationCase.name);
            assertTrue(command.getExecutionTime() >= 0L, operationCase.name);
        }
    }

    /**
     * Collects DynamoDB commands from every execution-tracer thread entry.
     *
     * @return recorded commands
     */
    private List<DynamoDbCommand> recordedCommands() {
        List<DynamoDbCommand> commands = new ArrayList<>();
        for (AdditionalInfo info : ExecutionTracer.exposeAdditionalInfoList()) {
            commands.addAll(info.getDynamoDbInfoData());
        }
        assertFalse(commands.isEmpty(), "Expected an intercepted low-level DynamoDB request");
        return commands;
    }

    /**
     * Finds the deepest preserved exception cause.
     *
     * @param throwable observed exception
     * @return deepest cause
     */
    private Throwable deepestCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    /**
     * Creates expected operation details.
     *
     * @param name fixture operation name
     * @param operationName expected DynamoDB operation
     * @param requestSimpleName expected generated low-level request type
     * @return operation case
     */
    private static OperationCase operation(String name, DynamoDbOperationNames operationName,
                                           String requestSimpleName) {
        return new OperationCase(name, operationName, requestSimpleName);
    }

    /**
     * Expected tracing details for one enhanced-client operation.
     */
    private static class OperationCase {

        private final String name;
        private final DynamoDbOperationNames operationName;
        private final String requestSimpleName;

        /**
         * @param name fixture operation name
         * @param operationName expected DynamoDB operation
         * @param requestSimpleName expected low-level request type
         */
        private OperationCase(String name, DynamoDbOperationNames operationName, String requestSimpleName) {
            this.name = name;
            this.operationName = operationName;
            this.requestSimpleName = requestSimpleName;
        }
    }
}
