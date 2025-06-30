package org.evomaster.client.java.instrumentation.example.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class RedisAsyncCommandsInstrumentedTest {

    private static final int REDIS_PORT = 6379;

    private static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.0")
            .withExposedPorts(REDIS_PORT);

    private static RedisClient client;
    private static StatefulRedisConnection<String, String> connection;
    private static RedisAsyncCommands<String, String> asyncCommands;

    private RedisAsyncCommandsOperations redisOps;

    protected RedisAsyncCommandsOperations getInstrumentedInstance() throws Exception {
        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        Class<?> clazz = cl.loadClass("com.foo.somedifferentpackage.examples.methodreplacement.redis.RedisAsyncCommandsOperationsImpl");

        Method setter = clazz.getMethod("setAsyncCommands", Object.class);
        setter.invoke(null, asyncCommands);

        return (RedisAsyncCommandsOperations) clazz.getDeclaredConstructor().newInstance();
    }

    @BeforeAll
    public static void setupRedis() {
        redisContainer.start();
        String host = redisContainer.getHost();
        int port = redisContainer.getMappedPort(REDIS_PORT);

        String redisUri = "redis://" + host + ":" + port;
        client = RedisClient.create(redisUri);
        connection = client.connect();
        asyncCommands = connection.async();
    }

    @BeforeEach
    public void resetTracer() throws Exception {
        redisOps = getInstrumentedInstance();
        ExecutionTracer.reset();
        ExecutionTracer.setExecutingInitRedis(false);

        RedisCommands<String, String> sync = connection.sync();
        sync.flushall();
    }

    @AfterEach
    public void checkInstrumentation() {
        assertTrue(ExecutionTracer.getNumberOfObjectives() > 0,
                "Expected at least one objective to be collected by the instrumentation");
    }

    @AfterAll
    public static void tearDown() {
        connection.close();
        client.shutdown();
        redisContainer.stop();
        ExecutionTracer.reset();
    }

    @Test
    public void testGetExecution() throws Exception {
        connection.sync().set("foo", "bar");
        String result = ((RedisFuture<String>) redisOps.get("foo")).get(2, TimeUnit.SECONDS);
        assertEquals("bar", result);
    }

    @Test
    public void testHgetExecution() throws Exception {
        connection.sync().hset("user:1", "name", "Alice");
        String result = ((RedisFuture<String>) redisOps.hget("user:1", "name")).get(2, TimeUnit.SECONDS);
        assertEquals("Alice", result);
    }

    @Test
    public void testHgetallExecution() throws Exception {
        connection.sync().hset("user:2", "name", "Bob");
        connection.sync().hset("user:2", "age", "30");

        Map<String, String> result = ((RedisFuture<Map<String, String>>) redisOps.hgetall("user:2")).get(2, TimeUnit.SECONDS);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Bob", result.get("name"));
        assertEquals("30", result.get("age"));
    }
}