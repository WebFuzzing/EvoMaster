package org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses;

import org.evomaster.client.java.instrumentation.AdditionalInfo;
import org.evomaster.client.java.instrumentation.RedisCommand;
import org.evomaster.client.java.instrumentation.staticstate.ExecutionTracer;
import org.junit.jupiter.api.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisKeyValueTemplate;
import org.springframework.data.redis.core.RedisKeyValueAdapter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.mapping.RedisMappingContext;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.testcontainers.containers.GenericContainer;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class RedisKeyValueTemplateClassReplacementTest {

    private static RedisKeyValueTemplate template;
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

        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        redisTemplate.afterPropertiesSet();

        RedisMappingContext mappingContext = new RedisMappingContext();
        mappingContext.afterPropertiesSet();

        RedisKeyValueAdapter adapter = new RedisKeyValueAdapter(redisTemplate, mappingContext);

        template = new RedisKeyValueTemplate(adapter, mappingContext);
        ExecutionTracer.reset();
    }

    @AfterEach
    public void cleanup() {
        Iterable<SimpleEntity> all = template.findAll(SimpleEntity.class);
        for (SimpleEntity entity : all) {
            template.delete(entity.getId(), SimpleEntity.class);
        }
        ExecutionTracer.reset();
    }

    @AfterAll
    public static void stop() {
        ExecutionTracer.reset();
        redisContainer.stop();
    }

    public static class SimpleEntity {
        @Id
        private String id;
        private String name;
        private int age;

        public SimpleEntity() {}

        public SimpleEntity(String id, String name, int age) {
            this.id = id;
            this.name = name;
            this.age = age;
        }

        public String getId() { return id; }

        public String getName() { return name; }

        public int getAge() { return age; }
    }

    @Test
    public void testFindByIdTracksExecution() throws Throwable {
        SimpleEntity entity = new SimpleEntity("abc123", "John", 30);
        template.insert(entity);

        ExecutionTracer.setExecutingInitRedis(false);

        Optional<SimpleEntity> result = RedisKeyValueTemplateClassReplacement.findById(template, "abc123", SimpleEntity.class);

        assertNotNull(result);
        assertTrue(result.isPresent());
        assertEquals("John", result.get().getName());

        List<AdditionalInfo> infos = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, infos.size());

        AdditionalInfo info = infos.get(0);

        Set<RedisCommand> commands = info.getRedisCommandData();
        assertEquals(1, commands.size());
        RedisCommand cmd = commands.iterator().next();
        assertEquals(RedisCommand.RedisCommandType.HGETALL, cmd.getType());
        assertEquals("abc123", cmd.getKey());
    }

    @Test
    public void testFindAllTracksExecution() throws Throwable {
        SimpleEntity entity = new SimpleEntity("xyz456", "Alice", 25);
        template.insert(entity);

        ExecutionTracer.setExecutingInitRedis(false);

        Iterable<SimpleEntity> all = RedisKeyValueTemplateClassReplacement.findAll(template, SimpleEntity.class);

        assertNotNull(all);
        assertTrue(all.iterator().hasNext());

        List<AdditionalInfo> infos = ExecutionTracer.exposeAdditionalInfoList();
        assertEquals(1, infos.size());

        AdditionalInfo info = infos.get(0);

        Set<RedisCommand> commands = info.getRedisCommandData();
        assertEquals(1, commands.size());
        RedisCommand cmd = commands.iterator().next();
        assertEquals(RedisCommand.RedisCommandType.HGETALL, cmd.getType());
        assertTrue(cmd.getKey().startsWith("ALL:"));
    }
}