package org.evomaster.client.java.instrumentation.example.redis;

import com.foo.somedifferentpackage.examples.methodreplacement.redis.RedisHashOperationsImpl;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class HashOperationsInstrumentedTest {

    private static final int REDIS_PORT = 6379;

    private static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.0")
            .withExposedPorts(REDIS_PORT);

    private static RedisTemplate<String, String> redisTemplate;

    private RedisHashOperations redisOps; // este es el proxy instrumentado

    protected RedisHashOperations getInstrumentedInstance() throws Exception {
        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        Class<?> clazz = cl.loadClass(RedisHashOperationsImpl.class.getName());

        // Set static RedisTemplate field usando reflection
        Method setter = clazz.getMethod("setRedisTemplate", Object.class);
        setter.invoke(null, redisTemplate);

        return (RedisHashOperations) clazz.getDeclaredConstructor().newInstance();
    }

    @BeforeAll
    public static void setupRedis() {
        redisContainer.start();
        String host = redisContainer.getHost();
        int port = redisContainer.getMappedPort(REDIS_PORT);

        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(host, port);
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();

        redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();
    }

    @BeforeEach
    public void resetTracer() throws Exception {
        redisOps = getInstrumentedInstance();
        ExecutionTracer.reset();
        ExecutionTracer.setExecutingInitRedis(false);
    }

    @AfterEach
    public void checkInstrumentation() {
        assertTrue(ExecutionTracer.getNumberOfObjectives() > 0,
                "Expected at least one objective to be collected by the instrumentation");
    }

    @AfterAll
    public static void tearDown() {
        redisContainer.stop();
        ExecutionTracer.reset();
    }

    @Test
    public void testOpsForHashGetReplacement() {

        redisTemplate.opsForHash().put("foo", "bar", "foobar");

        String result = redisOps.getValue("foo", "bar");
        assertEquals("foobar", result);

        int objectives = ExecutionTracer.getNumberOfObjectives();
        assertTrue(objectives > 0, "Expected objectives created by instrumented class via replacement");
    }

    @Test
    public void testOpsForHashGetWithMissingKey() {
        redisTemplate.delete("missingKey");

        String result = redisOps.getValue("missingKey", "missingHashKey");
        assertNull(result);

    }
}