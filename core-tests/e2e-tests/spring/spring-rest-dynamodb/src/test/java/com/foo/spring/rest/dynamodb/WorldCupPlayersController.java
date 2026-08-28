package com.foo.spring.rest.dynamodb;

import com.dynamodb.players.WorldCupPlayersApp;
import org.evomaster.client.java.controller.EmbeddedSutController;
import org.evomaster.client.java.controller.api.dto.SutInfoDto;
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto;
import org.evomaster.client.java.controller.problem.ProblemInfo;
import org.evomaster.client.java.controller.problem.RestProblem;
import org.evomaster.client.java.sql.DbSpecification;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * EvoMaster embedded controller for the DynamoDB Local E2E application.
 */
public class WorldCupPlayersController extends EmbeddedSutController {

    private static final int DYNAMODB_PORT = 8000;
    private static final String TABLE_NAME = "WorldCupPlayers";

    private final GenericContainer<?> dynamoDb = new GenericContainer<>("amazon/dynamodb-local:2.5.3")
            .withExposedPorts(DYNAMODB_PORT)
            .withCommand("-jar", "DynamoDBLocal.jar", "-inMemory", "-sharedDb")
            .waitingFor(Wait.forListeningPort());

    private ConfigurableApplicationContext context;
    private DynamoDbAsyncClient dynamoDbClient;

    /**
     * Creates the embedded controller on a random controller port.
     */
    public WorldCupPlayersController() {
        setControllerPort(0);
    }

    /**
     * Starts DynamoDB Local, seeds the table, and starts the SUT.
     *
     * @return base URL of the SUT
     */
    @Override
    public String startSut() {
        dynamoDb.start();
        String endpoint = "http://" + dynamoDb.getHost() + ":" + dynamoDb.getMappedPort(DYNAMODB_PORT);
        dynamoDbClient = createClient(endpoint);
        seedWorldCupPlayers();

        context = new SpringApplicationBuilder(WorldCupPlayersApp.class)
                .properties("server.port=0", "dynamodb.endpoint=" + endpoint)
                .run();
        return "http://localhost:" + getSutPort();
    }

    /**
     * Stops the SUT and DynamoDB Local resources.
     */
    @Override
    public void stopSut() {
        if (context != null) {
            context.close();
            context = null;
        }
        if (dynamoDbClient != null) {
            dynamoDbClient.close();
            dynamoDbClient = null;
        }
        if (dynamoDb.isRunning()) {
            dynamoDb.stop();
        }
    }

    /**
     * Leaves the seeded, read-only table unchanged between actions.
     */
    @Override
    public void resetStateOfSUT() {
        // The SUT only reads the immutable seed data.
    }

    /**
     * This SUT has no relational database specification.
     *
     * @return no database specifications
     */
    @Override
    public List<DbSpecification> getDbSpecifications() {
        return null;
    }

    /**
     * Reports whether the Spring application is running.
     *
     * @return true when the application context is running
     */
    @Override
    public boolean isSutRunning() {
        return context != null && context.isRunning();
    }

    /**
     * This SUT does not require authentication.
     *
     * @return an empty authentication list
     */
    @Override
    public List<AuthenticationDto> getInfoForAuthentication() {
        return Collections.emptyList();
    }

    /**
     * Describes the REST problem exposed by the SUT.
     *
     * @return REST problem information
     */
    @Override
    public ProblemInfo getProblemInfo() {
        return new RestProblem("http://localhost:" + getSutPort() + "/v3/api-docs", null);
    }

    /**
     * Uses EvoMaster's default generated-test output format.
     *
     * @return no preferred output format
     */
    @Override
    public SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return null;
    }

    /**
     * Selects the SUT package for instrumentation.
     *
     * @return package prefix to cover
     */
    @Override
    public String getPackagePrefixesToCover() {
        return "com.dynamodb.players";
    }

    /**
     * Exposes the DynamoDB connection used for heuristic scans.
     *
     * @return the DynamoDB async client
     */
    @Override
    public Object getDynamoDbConnection() {
        return dynamoDbClient;
    }

    /**
     * Reads the random HTTP port assigned by Spring.
     *
     * @return SUT HTTP port
     */
    private int getSutPort() {
        Map<?, ?> ports = (Map<?, ?>) Objects.requireNonNull(context.getEnvironment()
                .getPropertySources().get("server.ports")).getSource();
        return (Integer) ports.get("local.server.port");
    }

    /**
     * Creates a client for the given DynamoDB Local endpoint.
     *
     * @param endpoint DynamoDB Local endpoint
     * @return configured async client
     */
    private DynamoDbAsyncClient createClient(String endpoint) {
        return DynamoDbAsyncClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("local", "local")))
                .build();
    }

    /**
     * Creates and seeds the World Cup players table.
     */
    private void seedWorldCupPlayers() {
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
        dynamoDbClient.createTable(createTable).join();

        Map<String, AttributeValue> lionelMessi = new java.util.HashMap<>();
        lionelMessi.put("country", AttributeValue.builder().s("Argentina").build());
        lionelMessi.put("playerName", AttributeValue.builder().s("Lionel Messi").build());
        lionelMessi.put("fifaId", AttributeValue.builder().n("158023").build());
        lionelMessi.put("shirtNumber", AttributeValue.builder().n("10").build());
        dynamoDbClient.putItem(PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(lionelMessi)
                .build()).join();
    }
}
