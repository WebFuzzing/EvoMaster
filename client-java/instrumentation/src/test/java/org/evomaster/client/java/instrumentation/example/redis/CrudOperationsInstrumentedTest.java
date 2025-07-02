package org.evomaster.client.java.instrumentation.example.redis;

import com.foo.somedifferentpackage.examples.methodreplacement.redis.RedisCrudOperationsImpl;
import com.foo.somedifferentpackage.examples.methodreplacement.subclass.RedisCrudRepository;
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

public class CrudOperationsInstrumentedTest {

    private static final int REDIS_PORT = 6379;

    private static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.0")
            .withExposedPorts(REDIS_PORT);

    private static RedisCrudRepository repository;
    private static RedisTemplate<RedisEntity, String> redisTemplate;

    private RedisCrudOperations redisOps;

    protected RedisCrudOperations getInstrumentedInstance() throws Exception {
        InstrumentingClassLoader cl = new InstrumentingClassLoader("com.foo");

        Class<?> clazz = cl.loadClass(RedisCrudOperationsImpl.class.getName());

        Method setter = clazz.getMethod("setRepository", Object.class);
        setter.invoke(null, repository);

        return (RedisCrudOperations) clazz.getDeclaredConstructor().newInstance();
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

        repository = RedisCrudRepositorySetup.createRepository(redisTemplate);

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
    public void testCrudGetGeneratesObjectives() {
        RedisEntity e = new RedisEntity("123", "Test");
        repository.save(e);

        RedisEntity result = redisOps.findById("123").get();
        assertNotNull(result);

        int objectives = ExecutionTracer.getNumberOfObjectives();
        assertTrue(objectives > 0, "Expected objectives created by RedisKeyValueTemplateClassReplacement");
    }
}