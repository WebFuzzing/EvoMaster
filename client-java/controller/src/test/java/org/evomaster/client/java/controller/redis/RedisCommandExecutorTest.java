package org.evomaster.client.java.controller.redis;

import org.evomaster.client.java.controller.api.dto.database.operations.RedisInsertionDto;
import org.evomaster.client.java.controller.api.dto.database.operations.RedisInsertionResultsDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class RedisCommandExecutorTest {

    private static ReflectionBasedRedisClient client;

    private static final int REDIS_PORT = 6379;
    private static final GenericContainer<?> redis = new GenericContainer<>("redis:7.0")
            .withExposedPorts(REDIS_PORT);

    @BeforeAll
    public static void initClass() {
        redis.start();
        int port = redis.getMappedPort(REDIS_PORT);
        client = new ReflectionBasedRedisClient("localhost", port);
    }

    @AfterEach
    public void cleanUp() {
        client.flushAll();
    }

    @Test
    public void testInsertSingleKey() {
        RedisInsertionDto dto = new RedisInsertionDto();
        dto.key = "user:1";
        dto.value = "Alice";
        dto.keyspace = 0;

        RedisInsertionResultsDto results =
                RedisCommandExecutor.executeInsert(client, Collections.singletonList(dto));

        assertTrue(results.executionResults.get(0));
        assertEquals("Alice", client.getValue("user:1"));
    }

    @Test
    public void testInsertMultipleKeys() {
        RedisInsertionDto dto1 = new RedisInsertionDto();
        dto1.key = "product:1";
        dto1.value = "chair";
        dto1.keyspace = 0;

        RedisInsertionDto dto2 = new RedisInsertionDto();
        dto2.key = "product:2";
        dto2.value = "table";
        dto2.keyspace = 0;

        RedisInsertionResultsDto results =
                RedisCommandExecutor.executeInsert(client, Arrays.asList(dto1, dto2));

        assertTrue(results.executionResults.get(0));
        assertTrue(results.executionResults.get(1));
        assertEquals("chair", client.getValue("product:1"));
        assertEquals("table", client.getValue("product:2"));
    }

    @Test
    public void testInsertInNonDefaultKeyspace() {
        RedisInsertionDto dto = new RedisInsertionDto();
        dto.key = "session:abc";
        dto.value = "data";
        dto.keyspace = 1;

        RedisInsertionResultsDto results =
                RedisCommandExecutor.executeInsert(client, Collections.singletonList(dto));

        assertTrue(results.executionResults.get(0));
        // the executor left the connection in keyspace 1, so getValue reads from there
        assertEquals("data", client.getValue("session:abc"));
    }

    @Test
    public void testInsertNullListThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> RedisCommandExecutor.executeInsert(client, null));
    }

    @Test
    public void testInsertEmptyListThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> RedisCommandExecutor.executeInsert(client, Collections.emptyList()));
    }

    @Test
    public void testOverwritesExistingKey() {
        RedisInsertionDto first = new RedisInsertionDto();
        first.key = "overwrite:key";
        first.value = "old";
        first.keyspace = 0;
        RedisCommandExecutor.executeInsert(client, Collections.singletonList(first));

        RedisInsertionDto second = new RedisInsertionDto();
        second.key = "overwrite:key";
        second.value = "new";
        second.keyspace = 0;
        RedisCommandExecutor.executeInsert(client, Collections.singletonList(second));

        assertEquals("new", client.getValue("overwrite:key"));
    }
}