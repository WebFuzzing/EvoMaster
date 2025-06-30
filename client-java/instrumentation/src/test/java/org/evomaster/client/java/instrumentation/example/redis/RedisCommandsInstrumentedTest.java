package org.evomaster.client.java.instrumentation.example.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RedisCommandsInstrumentedTest {

    private static final int REDIS_PORT = 6379;

    private static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.0")
            .withExposedPorts(REDIS_PORT);

    private static RedisClient client;
    private static StatefulRedisConnection<String, String> connection;
    private static RedisCommands<String, String> syncCommands;

    private RedisCommandsOperations redisOps;

    protected RedisCommandsOperations getInstrumentedInstance() throws Exception {
        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        Class<?> clazz = cl.loadClass("com.foo.somedifferentpackage.examples.methodreplacement.redis.RedisCommandsOperationsImpl");

        Method setter = clazz.getMethod("setSyncCommands", Object.class);
        setter.invoke(null, syncCommands);

        return (RedisCommandsOperations) clazz.getDeclaredConstructor().newInstance();
    }

    @BeforeAll
    public static void setupRedis() {
        redisContainer.start();
        String host = redisContainer.getHost();
        int port = redisContainer.getMappedPort(REDIS_PORT);

        String redisUri = "redis://" + host + ":" + port;
        client = RedisClient.create(redisUri);
        connection = client.connect();
        syncCommands = connection.sync();
    }

    @BeforeEach
    public void resetTracer() throws Exception {
        redisOps = getInstrumentedInstance();
        ExecutionTracer.reset();
        ExecutionTracer.setExecutingInitRedis(false);
        syncCommands.flushall();
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
    public void testGetExecution() {
        syncCommands.set("foo", "bar");
        String result = redisOps.get("foo");
        assertEquals("bar", result);
    }

    @Test
    public void testHgetExecution() {
        syncCommands.hset("user:1", "name", "Alice");
        String result = redisOps.hget("user:1", "name");
        assertEquals("Alice", result);
    }

    @Test
    public void testHgetallExecution() {
        syncCommands.hset("user:2", "name", "Bob");
        syncCommands.hset("user:2", "age", "30");

        Map<String, String> result = redisOps.hgetall("user:2");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Bob", result.get("name"));
        assertEquals("30", result.get("age"));
    }
}