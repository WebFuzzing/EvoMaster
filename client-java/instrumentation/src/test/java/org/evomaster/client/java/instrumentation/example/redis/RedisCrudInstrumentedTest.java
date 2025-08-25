package org.evomaster.client.java.instrumentation.example.redis;

import com.foo.somedifferentpackage.examples.methodreplacement.redis.RedisCrudOperationsImpl;
import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.InputProperties;
import org.evomaster.client.java.instrumentation.InstrumentingClassLoader;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RedisCrudInstrumentedTest {

    private static final int REDIS_PORT = 6379;
    private static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.0")
            .withExposedPorts(REDIS_PORT);

    private static String defaultReplacement;

    @BeforeAll
    static void setupAll() {
        redisContainer.start();
        defaultReplacement = System.getProperty(InputProperties.REPLACEMENT_CATEGORIES);
        if (defaultReplacement != null) {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, defaultReplacement + ",REDIS");
        } else {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, "BASE,SQL,EXT_0,NET,MONGO,REDIS");
        }
    }

    @AfterAll
    static void teardownAll() {
        redisContainer.stop();
        if (defaultReplacement != null) {
            System.setProperty(InputProperties.REPLACEMENT_CATEGORIES, defaultReplacement);
        }
    }

    protected RedisCrudOperations getInstance() throws Exception {
        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");
        return (RedisCrudOperations)
                cl.loadClass(RedisCrudOperationsImpl.class.getName())
                        .getConstructor(int.class)
                        .newInstance(redisContainer.getMappedPort(REDIS_PORT));
    }

    @Test
    void testFindByIdCrudInstrumentation() throws Exception {
        ExecutionTracer.reset();
        RedisCrudOperations ops = getInstance();

        ops.findById("someId");

        List<AdditionalInfo> infoList = ExecutionTracer.exposeAdditionalInfoList();
        assertFalse(infoList.isEmpty(), "Expected Redis instrumentation data");

        boolean foundHGetAll = infoList.stream()
                .flatMap(i -> i.getRedisCommandData().stream())
                .anyMatch(cmd -> cmd.getType().name().equals("HGETALL"));

        assertTrue(foundHGetAll, "Expected a HGETALL command to be instrumented");
    }

    @Test
    void testFindAllCrudInstrumentation() throws Exception {
        ExecutionTracer.reset();
        RedisCrudOperations ops = getInstance();

        ops.findAll();

        List<AdditionalInfo> infoList = ExecutionTracer.exposeAdditionalInfoList();
        assertFalse(infoList.isEmpty(), "Expected Redis instrumentation data");

        boolean foundSMembers = infoList.stream()
                .flatMap(i -> i.getRedisCommandData().stream())
                .anyMatch(cmd -> cmd.getType().name().equals("SMEMBERS"));

        assertTrue(foundSMembers, "Expected a SMEMBERS command to be instrumented");
    }
}
