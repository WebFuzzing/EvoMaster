package com.foo.spring.rest.dynamodb;

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

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shared embedded-controller lifecycle for DynamoDB Local end-to-end applications.
 */
public abstract class DynamoDbController extends EmbeddedSutController {

    private static final int DYNAMODB_PORT = 8000;

    private final GenericContainer<?> dynamoDb = new GenericContainer<>("amazon/dynamodb-local:2.5.3")
            .withExposedPorts(DYNAMODB_PORT)
            .withCommand("-jar", "DynamoDBLocal.jar", "-inMemory", "-sharedDb")
            .waitingFor(Wait.forListeningPort());

    private ConfigurableApplicationContext context;
    private DynamoDbAsyncClient dynamoDbClient;

    /**
     * Creates an embedded controller on a random controller port.
     */
    protected DynamoDbController() {
        setControllerPort(0);
    }

    /**
     * Returns the Spring application class to start.
     *
     * @return SUT application class
     */
    protected abstract Class<?> getApplicationClass();

    /**
     * Creates and seeds the database before the SUT starts.
     *
     * @param client DynamoDB Local administration client
     */
    protected abstract void initializeDatabase(DynamoDbAsyncClient client);

    /**
     * Restores mutable database state between generated test cases.
     *
     * @param client DynamoDB Local administration client
     */
    protected void resetDatabase(DynamoDbAsyncClient client) {
        // Read-only fixtures do not need reset work.
    }

    /**
     * Starts DynamoDB Local, initializes its data, and starts the SUT.
     *
     * @return base URL of the SUT
     */
    @Override
    public final String startSut() {
        dynamoDb.start();
        String endpoint = "http://" + dynamoDb.getHost() + ":" + dynamoDb.getMappedPort(DYNAMODB_PORT);
        dynamoDbClient = createClient(endpoint);
        initializeDatabase(dynamoDbClient);

        context = new SpringApplicationBuilder(getApplicationClass())
                .properties("server.port=0", "dynamodb.endpoint=" + endpoint)
                .run();
        return "http://localhost:" + getSutPort();
    }

    /**
     * Stops the SUT and DynamoDB Local resources.
     */
    @Override
    public final void stopSut() {
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
     * Restores the database fixture for the next generated test case.
     */
    @Override
    public final void resetStateOfSUT() {
        if (dynamoDbClient != null) {
            resetDatabase(dynamoDbClient);
        }
    }

    /**
     * This SUT has no relational database specification.
     *
     * @return no database specifications
     */
    @Override
    public final List<DbSpecification> getDbSpecifications() {
        return null;
    }

    /**
     * Reports whether the Spring application is running.
     *
     * @return true when the application context is running
     */
    @Override
    public final boolean isSutRunning() {
        return context != null && context.isRunning();
    }

    /**
     * This SUT does not require authentication.
     *
     * @return an empty authentication list
     */
    @Override
    public final List<AuthenticationDto> getInfoForAuthentication() {
        return Collections.emptyList();
    }

    /**
     * Describes the REST problem exposed by the SUT.
     *
     * @return REST problem information
     */
    @Override
    public final ProblemInfo getProblemInfo() {
        return new RestProblem("http://localhost:" + getSutPort() + "/v2/api-docs", null);
    }

    /**
     * Uses EvoMaster's default generated-test output format.
     *
     * @return no preferred output format
     */
    @Override
    public final SutInfoDto.OutputFormat getPreferredOutputFormat() {
        return null;
    }

    /**
     * Exposes the DynamoDB connection used for heuristic scans.
     *
     * @return DynamoDB async administration client
     */
    @Override
    public final Object getDynamoDbConnection() {
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
     * Creates an asynchronous administration client for DynamoDB Local.
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
}
