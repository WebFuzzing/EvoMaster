package com.foo.somedifferentpackage.examples.methodreplacement;

import org.evomaster.client.java.instrumentation.example.redis.RedisEntity;
import org.evomaster.client.java.instrumentation.example.redis.RedisCrudOperations;

import java.lang.reflect.Method;
import java.util.Optional;

public class RedisCrudOperationsImpl implements RedisCrudOperations {

    private static Object repository;

    public static void setRepository(Object repo) {
        repository = repo;
    }

    @Override
    public Optional<RedisEntity> findById(String id) {
        try {
            Method findById = repository.getClass().getMethod("findById", Object.class);
            findById.setAccessible(true);
            return (Optional<RedisEntity>) findById.invoke(repository, id);
        } catch (Exception e) {
            throw new RuntimeException("Reflection call failed", e);
        }
    }
}