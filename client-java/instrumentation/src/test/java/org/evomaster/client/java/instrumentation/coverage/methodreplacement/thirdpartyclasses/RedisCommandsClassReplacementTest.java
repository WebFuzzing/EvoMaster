package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.RedisCommand;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RedisCommandsClassReplacementTest {

    private static final int REDIS_PORT = 6379;
    private static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.0")
            .withExposedPorts(REDIS_PORT);

    private static RedisClient redisClient;
    private static RedisCommands<String, String> commands;
    private static StatefulRedisConnection<String, String> connection;

    @BeforeAll
    public static void setup() {
        redisContainer.start();
        String host = redisContainer.getHost();
        int port = redisContainer.getMappedPort(REDIS_PORT);

        redisClient = RedisClient.create("redis://" + host + ":" + port);
        connection = redisClient.connect();
        commands = connection.sync();

        ExecutionTracer.reset();
    }

    @AfterEach
    public void cleanUp() {
        commands.flushall();
        ExecutionTracer.reset();
    }

    @AfterAll
    public static void teardown() {
        connection.close();
        redisClient.shutdown();
        redisContainer.stop();
    }

    @Test
    public void testGetExecution() {
        String key = "foo";
        String value = "bar";
        commands.set(key, value);

        ExecutionTracer.setExecutingInitRedis(false);
        String result = RedisCommandsClassReplacement.get(commands, key);

        assertEquals(value, result);

        List<AdditionalInfo> infoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, infoList.size());

        RedisCommand cmd = infoList.get(0).getRedisCommandData().iterator().next();
        assertEquals(RedisCommand.RedisCommandType.GET, cmd.getType());
        assertEquals(key, cmd.getKey());
    }

    @Test
    public void testHgetExecution() {
        String key = "user:1";
        String hashKey = "name";
        String value = "Alice";
        commands.hset(key, hashKey, value);

        ExecutionTracer.setExecutingInitRedis(false);
        String result = RedisCommandsClassReplacement.hget(commands, key, hashKey);

        assertEquals(value, result);

        List<AdditionalInfo> infoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, infoList.size());

        RedisCommand cmd = infoList.get(0).getRedisCommandData().iterator().next();
        assertEquals(RedisCommand.RedisCommandType.HGET, cmd.getType());
        assertEquals(key, cmd.getKey());
        assertEquals(hashKey, cmd.getSubKey());
    }

    @Test
    public void testHgetallExecution() {
        String key = "user:2";
        commands.hset(key, "name", "Bob");
        commands.hset(key, "age", "42");

        ExecutionTracer.setExecutingInitRedis(false);
        Map<String, String> result = RedisCommandsClassReplacement.hgetall(commands, key);

        assertEquals(2, result.size());
        assertEquals("Bob", result.get("name"));

        List<AdditionalInfo> infoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, infoList.size());

        RedisCommand cmd = infoList.get(0).getRedisCommandData().iterator().next();
        assertEquals(RedisCommand.RedisCommandType.HGETALL, cmd.getType());
        assertEquals(key, cmd.getKey());
    }
}