package org.evomaster.client.java.instrumentation.example.redis;

import com.foo.somedifferentpackage.examples.methodreplacement.redis.JedisOperationsImpl;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.InputProperties;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JedisInstrumentedTest {

    private static String defaultReplacement;

    private static final int REDIS_PORT = 6379;

    // redis-stack-server, not plain redis, so the RediSearch module is loaded
    // server-side and FT.CREATE/FT.SEARCH don't error out
    private static final GenericContainer<?> redisContainer =
            new GenericContainer<>("redis/redis-stack-server:latest")
                    .withExposedPorts(REDIS_PORT);

    private static final String GET = "GET";
    private static final String FT_SEARCH = "FT_SEARCH";

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

    private JedisOperations getInstance() throws Exception {
        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");
        return (JedisOperations) cl.loadClass(JedisOperationsImpl.class.getName())
                .getConstructor(String.class, int.class)
                .newInstance(redisContainer.getHost(), redisContainer.getMappedPort(REDIS_PORT));
    }

    @Test
    public void testGetInstrumentationWithClassLoader() throws Exception {
        ExecutionTracer.reset();

        JedisOperations jedisInstrumented = getInstance();
        jedisInstrumented.get("foo");

        List<AdditionalInfo> infoList = ExecutionTracer.exposeAdditionalInfoList();
        assertFalse(infoList.isEmpty(), "Expected Redis instrumentation data");

        boolean foundGet = infoList.stream()
                .flatMap(i -> i.getRedisCommandData().stream())
                .anyMatch(cmd -> cmd.getType().name().equals(GET));

        assertTrue(foundGet, "Expected a GET command to be instrumented via ConnectionClassReplacement");
    }

    @Test
    public void testFtSearchInstrumentationWithClassLoader() throws Exception {
        ExecutionTracer.reset();

        JedisOperations jedisInstrumented = getInstance();
        jedisInstrumented.ftCreate("idx:products", "product:", "title");
        jedisInstrumented.hset("product:1", "title", "redis handbook");
        long total = jedisInstrumented.ftSearch("idx:products", "@title:redis");

        assertEquals(1, total, "Expected the indexed document to be found by FT.SEARCH");

        List<AdditionalInfo> infoList = ExecutionTracer.exposeAdditionalInfoList();
        assertFalse(infoList.isEmpty(), "Expected Redis instrumentation data");

        boolean foundFtSearch = infoList.stream()
                .flatMap(i -> i.getRedisCommandData().stream())
                .anyMatch(cmd -> cmd.getType().name().equals(FT_SEARCH));

        assertTrue(foundFtSearch, "Expected an FT.SEARCH command to be instrumented via ConnectionClassReplacement");
    }
}
