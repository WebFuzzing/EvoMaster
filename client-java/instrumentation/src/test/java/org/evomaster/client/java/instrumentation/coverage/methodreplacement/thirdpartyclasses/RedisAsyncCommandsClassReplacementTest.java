package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.RedisCommand;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class RedisAsyncCommandsClassReplacementTest {

    private static final int REDIS_PORT = 6379;
    private static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.0")
            .withExposedPorts(REDIS_PORT);

    private static RedisClient redisClient;
    private static RedisAsyncCommands<String, String> asyncCommands;
    private static StatefulRedisConnection<String, String> connection;

    @BeforeAll
    public static void setup() throws Exception {
        redisContainer.start();
        String host = redisContainer.getHost();
        int port = redisContainer.getMappedPort(REDIS_PORT);

        redisClient = RedisClient.create("redis://" + host + ":" + port);
        connection = redisClient.connect();
        asyncCommands = connection.async();

        ExecutionTracer.reset();
    }

    @AfterEach
    public void cleanup() throws Exception {
        asyncCommands.flushall().get();
        ExecutionTracer.reset();
    }

    @AfterAll
    public static void teardown() {
        connection.close();
        redisClient.shutdown();
        redisContainer.stop();
    }

    @Test
    public void testGetExecution() throws ExecutionException, InterruptedException {
        String key = "foo";
        String value = "bar";
        asyncCommands.set(key, value).get();

        ExecutionTracer.setExecutingInitRedis(false);
        RedisFuture<String> future = (RedisFuture<String>) RedisAsyncCommandsClassReplacement.get(asyncCommands, key);
        String result = future.get();

        assertEquals(value, result);

        List<AdditionalInfo> infoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, infoList.size());

        RedisCommand cmd = infoList.get(0).getRedisCommandData().iterator().next();
        assertEquals(RedisCommand.RedisCommandType.GET, cmd.getType());
        assertEquals(key, cmd.getKey());
    }

    @Test
    public void testHgetExecution() throws ExecutionException, InterruptedException {
        String key = "user:async";
        String hashKey = "name";
        String value = "Alice";
        asyncCommands.hset(key, hashKey, value).get();

        ExecutionTracer.setExecutingInitRedis(false);
        RedisFuture<String> future =
                (RedisFuture<String>) RedisAsyncCommandsClassReplacement.hget(asyncCommands, key, hashKey);
        String result = future.get();

        assertEquals(value, result);

        List<AdditionalInfo> infoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, infoList.size());

        RedisCommand cmd = infoList.get(0).getRedisCommandData().iterator().next();
        assertEquals(RedisCommand.RedisCommandType.HGET, cmd.getType());
        assertEquals(key, cmd.getKey());
        assertEquals(hashKey, cmd.getSubKey());
    }

    @Test
    public void testHgetallExecution() throws ExecutionException, InterruptedException {
        String key = "user:map";
        asyncCommands.hset(key, "field1", "A").get();
        asyncCommands.hset(key, "field2", "B").get();

        ExecutionTracer.setExecutingInitRedis(false);
        RedisFuture<Map<String, String>> future =
                (RedisFuture<Map<String, String>>) RedisAsyncCommandsClassReplacement.hgetall(asyncCommands, key);
        Map<String, String> result = future.get();

        assertEquals(2, result.size());
        assertEquals("A", result.get("field1"));

        List<AdditionalInfo> infoList = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, infoList.size());

        RedisCommand cmd = infoList.get(0).getRedisCommandData().iterator().next();
        assertEquals(RedisCommand.RedisCommandType.HGETALL, cmd.getType());
        assertEquals(key, cmd.getKey());
    }
}
