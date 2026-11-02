package com.foo.somedifferentpackage.examples.methodreplacement.redis;

import org.evomaster.client.java.instrumentation.example.redis.RedisMappingContextOperations;
import org.springframework.data.redis.core.mapping.RedisMappingContext;

public class RedisMappingContextOperationsImpl implements RedisMappingContextOperations {

    @Override
    public void createPersistentEntity(String entityClassName) throws ClassNotFoundException {
        Class<?> entity = Class.forName(entityClassName);
        // Spring builds the persistent entity of a type, and so calls the replaced BasicRedisPersistentEntity
        // constructor, the first time the mapping context is asked for it
        new RedisMappingContext().getRequiredPersistentEntity(entity);
    }
}
