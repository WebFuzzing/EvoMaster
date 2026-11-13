package org.evomaster.client.java.instrumentation.example.dynamodb;

import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbCommand;
import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbOperationNames;
import org.evomaster.client.java.instrumentation.dynamodb.DynamoDbQueryValue;
import org.evomaster.client.java.instrumentation.InputProperties;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterEach;
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

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks that the Function.apply option still observes each real paginator request.
 */
public class DynamoDbEnhancedPaginatorInstrumentedTest {

    private static final int DYNAMODB_PORT = 28001;
    private static final String REPLACEMENT_CATEGORIES = "BASE,DYNAMODB";
    private static final String EXAMPLE_PACKAGE = "com.foo.somedifferentpackage.examples.dynamodb";
    private static final String EXAMPLE_CLASS = EXAMPLE_PACKAGE + ".EnhancedFunctionApplyExample";
    private static final String READ_PLAYER_PAGES = "readPlayerPages";
    private static final String QUERY = "query";
    private static final String SCAN = "scan";
    private static final String BATCH_GET = "batch-get";
    private static final GenericContainer<?> dynamoDb = new GenericContainer<>("amazon/dynamodb-local:latest")
            .withExposedPorts(DYNAMODB_PORT)
            .withCommand("-jar", "DynamoDBLocal.jar", "-inMemory", "-port", String.valueOf(DYNAMODB_PORT))
            .waitingFor(Wait.forListeningPort());
    private static String endpoint;
    private static String tableName;
    private static int tableNumber;
    private String previousCategories;

    /** Creates a two-player table that needs multiple query pages with a one-item limit. */
    @BeforeAll
    public static void prepareTable() {
        dynamoDb.start();
        endpoint = "http://localhost:" + dynamoDb.getMappedPort(DYNAMODB_PORT);
        tableName = "WorldCupPlayers" + (++tableNumber);
        try (DynamoDbClient client = DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("dummy", "dummy")))
                .build()) {
            client.createTable(CreateTableRequest.builder()
                    .tableName(tableName)
                    .keySchema(
                            KeySchemaElement.builder().attributeName("country").keyType(KeyType.HASH).build(),
                            KeySchemaElement.builder().attributeName("name").keyType(KeyType.RANGE).build())
                    .attributeDefinitions(
                            AttributeDefinition.builder().attributeName("country").attributeType(ScalarAttributeType.S).build(),
                            AttributeDefinition.builder().attributeName("name").attributeType(ScalarAttributeType.S).build())
                    .provisionedThroughput(ProvisionedThroughput.builder()
                            .readCapacityUnits(5L).writeCapacityUnits(5L).build())
                    .build());
            putPlayer(client, "Lionel Messi");
            putPlayer(client, "Angel Di Maria");
        }
    }

    /** Stores one player in the query table. */
    private static void putPlayer(DynamoDbClient client, String name) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("country", AttributeValue.builder().s("Argentina").build());
        item.put("name", AttributeValue.builder().s(name).build());
        client.putItem(PutItemRequest.builder().tableName(tableName).item(item).build());
    }

    /** Stops DynamoDB Local after the test class completes. */
    @AfterAll
    public static void stopDynamoDb() {
        if (dynamoDb.isRunning()) {
            dynamoDb.stop();
        }
    }

    /** Enables tracking and clears setup commands. */
    @BeforeEach
    public void setup() {
        previousCategories = System.getProperty(InputProperties.REPLACEMENT_CATEGORIES);
        System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, REPLACEMENT_CATEGORIES);
        ExecutionTracer.reset();
    }

    /** Restores the replacement categories after the test. */
    @AfterEach
    public void teardown() {
        if (previousCategories == null) {
            System.clearProperty(InputProperties.REPLACEMENT_CATEGORIES);
        } else {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, previousCategories);
        }
    }

    /** Each consumed page must produce a distinct low-level query command. */
    @Test
    public void testObserveEachQueryPage() throws Exception {
        List<DynamoDbCommand> commands = readAndVerifyPages(QUERY, false, DynamoDbOperationNames.QUERY);
        assertTrue(commands.size() >= 2);
    }

    /** An asynchronous query must record every executed low-level page request. */
    @Test
    public void testObserveEachAsyncQueryPage() throws Exception {
        List<DynamoDbCommand> commands = readAndVerifyPages(QUERY, true, DynamoDbOperationNames.QUERY);
        assertTrue(commands.size() >= 2);
    }

    /** Scan must record each executed page in both client modes. */
    @Test
    public void testObserveEachScanPage() throws Exception {
        for (boolean async : new boolean[]{false, true}) {
            ExecutionTracer.reset();
            List<DynamoDbCommand> commands = readAndVerifyPages(SCAN, async, DynamoDbOperationNames.SCAN);
            assertTrue(commands.size() >= 2, async ? "async" : "sync");
        }
    }

    /** Batch-get must record the request containing both player keys. */
    @Test
    public void testObserveBatchGetRequests() throws Exception {
        for (boolean async : new boolean[]{false, true}) {
            ExecutionTracer.reset();
            List<DynamoDbCommand> commands = readAndVerifyPages(
                    BATCH_GET, async, DynamoDbOperationNames.BATCH_GET_ITEM);
            assertEquals(1, commands.size(), async ? "async" : "sync");
            List<Map<String, DynamoDbQueryValue>> keys =
                    commands.get(0).getDdbRequest().batchRequestItems().get(tableName).keys();
            assertEquals(2, keys.size());
            assertTrue(keys.stream().allMatch(key -> "Argentina".equals(key.get("country").getPayload())));
            assertTrue(keys.stream().anyMatch(key -> "Lionel Messi".equals(key.get("name").getPayload())));
            assertTrue(keys.stream().anyMatch(key -> "Angel Di Maria".equals(key.get("name").getPayload())));
        }
    }

    /** Invokes an instrumented Enhanced Client and checks each traced low-level request. */
    private List<DynamoDbCommand> readAndVerifyPages(String operation, boolean async,
                                                     DynamoDbOperationNames expected) throws Exception {
        InstrumentingClassLoader loader = new InstrumentingClassLoader(EXAMPLE_PACKAGE);
        Class<?> example = loader.loadClass(EXAMPLE_CLASS);
        Method read = example.getMethod(READ_PLAYER_PAGES, String.class, String.class, String.class, boolean.class);
        int pages = (int) read.invoke(null, endpoint, tableName, operation, async);
        List<DynamoDbCommand> commands = new ArrayList<>();
        for (AdditionalInfo info : ExecutionTracer.exposeAdditionalInfoList()) {
            commands.addAll(info.getDynamoDbInfoData());
        }

        String context = operation + (async ? " async" : " sync");
        assertTrue(pages > 0, context);
        assertEquals(pages, commands.size(), context);
        assertTrue(commands.stream().allMatch(command ->
                command.getOperationName() == expected
                        && command.isSuccessfullyExecuted()
                        && command.getTableNames().contains(tableName)), context);
        if (!BATCH_GET.equals(operation)) {
            assertTrue(commands.stream().allMatch(command -> tableName.equals(command.getDdbRequest().tableName())), context);
        }
        if (QUERY.equals(operation)) {
            assertTrue(commands.stream().allMatch(command ->
                    command.getDdbRequest().expressionAttributeValues().values().stream()
                            .anyMatch(value -> "Argentina".equals(value.getPayload()))), context);
        }
        return commands;
    }
}
