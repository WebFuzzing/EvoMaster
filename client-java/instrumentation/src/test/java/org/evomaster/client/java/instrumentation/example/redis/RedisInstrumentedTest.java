package org.evomaster.client.java.instrumentation.example.redis;

import com.foo.somedifferentpackage.examples.methodreplacement.redis.RedisOperationsImpl;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.InputProperties;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RedisInstrumentedTest {

    private static String defaultReplacement;
    private static final int REDIS_PORT = 6379;
    private static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.0")
            .withExposedPorts(REDIS_PORT);

    private static final String GET = "GET";
    private static final String HGET = "HGET";

    @BeforeAll
    public static void setupAll() {
        redisContainer.start();
        defaultReplacement = System.getProperty(InputProperties.REPLACEMENT_CATEGORIES);
        if (defaultReplacement != null) {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, defaultReplacement + ",REDIS");
        } else {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE,SQL,EXT_0,NET,MONGO,REDIS");
        }
    }

    @AfterAll
    public static void teardownAll() {
        redisContainer.stop();
        if (defaultReplacement != null) {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, defaultReplacement);
        }
    }

    protected RedisOperations getInstance() throws Exception {
        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        return (RedisOperations)
                cl.loadClass(RedisOperationsImpl.class.getName())
                        .getConstructor(String.class)
                        .newInstance("redis://" + redisContainer.getHost() + ":" + redisContainer.getMappedPort(REDIS_PORT));
    }

    @Test
    public void testGetInstrumentationWithClassLoader() throws Exception {
        ExecutionTracer.reset();
        RedisOperations redisInstrumented = getInstance();
        redisInstrumented.get("foo");
        List<AdditionalInfo> infoList = ExecutionTracer.exposeAdditionalInfoList();
        assertFalse(infoList.isEmpty(), "Expected Redis instrumentation data");

        boolean foundGet = infoList.stream()
                .flatMap(i -> i.getRedisCommandData().stream())
                .anyMatch(cmd -> cmd.getType().name().equals(GET));

        assertTrue(foundGet, "Expected a GET command to be instrumented");
    }

    @Test
    public void testHGetInstrumentationWithClassLoader() throws Exception {
        ExecutionTracer.reset();
        RedisOperations redisInstrumented = getInstance();
        redisInstrumented.hget("foo", "bar");
        List<AdditionalInfo> infoList = ExecutionTracer.exposeAdditionalInfoList();
        assertFalse(infoList.isEmpty(), "Expected Redis instrumentation data");

        boolean foundGet = infoList.stream()
                .flatMap(i -> i.getRedisCommandData().stream())
                .anyMatch(cmd -> cmd.getType().name().equals(HGET));

        assertTrue(foundGet, "Expected a GET command to be instrumented");
    }
}
