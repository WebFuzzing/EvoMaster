package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.RedisCommand;
import org.evomaster.client.java.instrumentation.RedisKeySchema;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class HashOperationsClassReplacementTest {

    private static RedisTemplate<String, Object> redisTemplate;
    private static HashOperations<String, String, Object> hashOps;
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
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.afterPropertiesSet();

        hashOps = redisTemplate.opsForHash();

        ExecutionTracer.reset();
    }

    @AfterAll
    public static void resetExecutionTracer() {
        ExecutionTracer.reset();
    }

    @AfterEach
    public void cleanup() {
        redisTemplate.getConnectionFactory().getConnection().flushAll();
        ExecutionTracer.reset();
    }

    @Test
    public void testHashOperationsGetTracksExecution() throws Throwable {
        String key = "testHashKey";
        String hashKey = "field1";
        String value = "someValue";

        // Prepopulate Redis
        hashOps.put(key, hashKey, value);

        ExecutionTracer.setExecutingInitRedis(false);

        // Instrumented call
        String result = HashOperationsClassReplacement.get(hashOps, key, hashKey);

        assertEquals(value, result);

        List<AdditionalInfo> infos = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, infos.size());

        AdditionalInfo info = infos.get(0);

        // Verify RedisCommand
        Set<RedisCommand> commands = info.getRedisCommandData();
        assertEquals(1, commands.size());

        RedisCommand cmd = commands.iterator().next();
        assertEquals(RedisCommand.RedisCommandType.HGET, cmd.getType());
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