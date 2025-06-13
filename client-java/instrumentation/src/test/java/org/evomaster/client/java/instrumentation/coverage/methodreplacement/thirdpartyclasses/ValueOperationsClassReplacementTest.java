package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.RedisCommand;
import org.evomaster.client.java.instrumentation.RedisKeySchema;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ValueOperations;
import org.testcontainers.containers.GenericContainer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ValueOperationsClassReplacementTest {

    private static RedisTemplate<String, String> redisTemplate;
    private static ValueOperations<String, String> valueOps;
    private static final int REDIS_PORT = 6379;
    private static final GenericContainer<?> redisContainer = new GenericContainer<>("redis:7.0")
            .withExposedPorts(REDIS_PORT);

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
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.afterPropertiesSet();

        valueOps = redisTemplate.opsForValue();

        ExecutionTracer.reset();
    }

    @AfterAll
    public static void resetExecutionTracer() {
        ExecutionTracer.reset();
    }

    @AfterEach
    public void resetExecutionTracerEach() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        ExecutionTracer.reset();
    }

    @Test
    public void testValueOperationsGetTracksExecution() throws Throwable {
        String key = "testKey";
        String value = "testValue";
        valueOps.set(key, value);

        ExecutionTracer.setExecutingInitRedis(false);

        // Instrumented call
        String result = ValueOperationsClassReplacement.get(valueOps, key);

        assertEquals(value, result);

        List<AdditionalInfo> infos = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, infos.size());

        AdditionalInfo info = infos.get(0);

        // Verify RedisCommand
        Set<RedisCommand> commands = info.getRedisCommandData();
        assertEquals(1, commands.size());

        RedisCommand cmd = commands.iterator().next();
        assertEquals(RedisCommand.RedisCommandType.GET, cmd.getType());
        assertEquals(key, cmd.getKey());
        assertEquals(value, cmd.getActualValue());
        assertEquals(value.getClass(), cmd.getValueType());

        // Verify RedisKeySchema
        Set<RedisKeySchema> schemas = info.getRedisKeyTypeData();
        assertEquals(1, schemas.size());

        RedisKeySchema schema = schemas.iterator().next();
        assertEquals(key, schema.getKeyName());
        assertTrue(schema.getSchemaJson().contains("type"));
    }
}
