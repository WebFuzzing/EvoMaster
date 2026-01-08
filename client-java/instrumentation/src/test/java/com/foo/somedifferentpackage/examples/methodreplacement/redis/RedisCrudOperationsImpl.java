package com.foo.somedifferentpackage.examples.methodreplacement.redis;

import org.evomaster.client.java.instrumentation.example.redis.RedisCrudOperations;
import org.evomaster.client.java.instrumentation.example.redis.RedisEntity;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisKeyValueAdapter;
import org.springframework.data.redis.core.RedisKeyValueTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.mapping.RedisMappingContext;

import java.util.Optional;

public class RedisCrudOperationsImpl implements RedisCrudOperations {

    private final RedisCrudRepositoryMock repository;

    public RedisCrudOperationsImpl(int port) {
        System.setProperty("io.lettuce.core.jfr", "false");

        LettuceConnectionFactory factory = new LettuceConnectionFactory("localhost", port);
        factory.afterPropertiesSet();

        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.afterPropertiesSet();

        RedisKeyValueAdapter adapter = new RedisKeyValueAdapter(redisTemplate);
        RedisMappingContext mappingContext = new RedisMappingContext();

        RedisKeyValueTemplate keyValueTemplate = new RedisKeyValueTemplate(adapter, mappingContext);

        this.repository = new RedisCrudRepositoryMock(keyValueTemplate);
    }

    @Override
    public Optional<RedisEntity> findById(String id) {
        return this.repository.findById(id);
    }

    @Override
    public Iterable<RedisEntity> findAll() {
        return this.repository.findAll();
    }
}
