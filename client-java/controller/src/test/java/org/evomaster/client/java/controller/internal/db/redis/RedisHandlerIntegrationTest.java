package org.evomaster.client.java.controller.internal.db.redis;

import org.evomaster.client.java.instrumentation.RedisCommand;
import org.evomaster.client.java.controller.redis.RedisClient;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisHandlerIntegrationTest {

    private static final int REDIS_PORT = 6379;
    private GenericContainer<?> redisContainer;
    private RedisClient client;
    private RedisHandler handler;
    private int port;

    @BeforeAll
    void setupContainer() {
        redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7.0"))
                .withExposedPorts(REDIS_PORT);
        redisContainer.start();

        port = redisContainer.getMappedPort(REDIS_PORT);

        client = new RedisClient("localhost", port);
    }

    @BeforeEach
    void setupHandler() {
        handler = new RedisHandler();
        handler.setRedisClient(client);
        handler.setCalculateHeuristics(true);
        handler.setExtractRedisExecution(true);
        client.flushAll();
    }

    @AfterAll
    void teardown() {
        redisContainer.stop();
    }

    @Test
    void testHeuristicDistanceForStringExists() {
        client.setValue("user:1", "John");
        client.setValue("user:2", "Jane");
        assertEquals(2, client.getAllKeys().size());

        RedisCommand similarKeyCmd = new RedisCommand(
                RedisCommand.RedisCommandType.EXISTS,
                new String[]{"key<user:3>"},
                true,
                10
        );
        RedisCommand differentKeyCmd = new RedisCommand(
                RedisCommand.RedisCommandType.EXISTS,
                new String[]{"key<user:82bd3bff-4567-40f4-a42e-27f87276199f>"},
                true,
                10
        );

        handler.handle(similarKeyCmd);
        handler.handle(differentKeyCmd);

        List<RedisCommandEvaluation> evals = handler.getEvaluatedRedisCommands();
        assertEquals(2, evals.size(), "Should be two command evaluations.");

        RedisCommandEvaluation evalForSimilar = evals.get(0);
        assertNotNull(evalForSimilar.getRedisDistanceWithMetrics());
        RedisCommandEvaluation evalForDifferent = evals.get(1);
        assertNotNull(evalForDifferent.getRedisDistanceWithMetrics());

        double distanceForSimilar = evalForSimilar.getRedisDistanceWithMetrics().getDistance();
        int evaluatedForSimilar = evalForSimilar.getRedisDistanceWithMetrics().getNumberOfEvaluatedKeys();

        assertTrue(distanceForSimilar >= 0 && distanceForSimilar <= 1,
                "Distance should be between 0 and 1");
        assertEquals(2, evaluatedForSimilar, "Both keys should be evaluated.");

        double distanceForDifferent = evalForDifferent.getRedisDistanceWithMetrics().getDistance();
        int evaluatedForDifferent = evalForDifferent.getRedisDistanceWithMetrics().getNumberOfEvaluatedKeys();

        assertTrue(distanceForDifferent >= 0 && distanceForDifferent <= 1,
                "Distance should be between 0 and 1");
        assertEquals(2, evaluatedForDifferent, "Both keys should be evaluated.");
        assertTrue(distanceForSimilar < distanceForDifferent,
                "Distance for similar should be the smallest.");
    }

    @Test
    void testResetClearsCommands() {
        RedisCommand cmd = new RedisCommand(
                RedisCommand.RedisCommandType.EXISTS,
                new String[]{"key<user:1>"},
                true,
                5
        );
        handler.handle(cmd);
        assertFalse(handler.getEvaluatedRedisCommands().isEmpty());
        handler.reset();
        assertTrue(handler.getEvaluatedRedisCommands().isEmpty());
    }
}