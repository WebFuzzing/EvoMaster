package com.foo.somedifferentpackage.examples.methodreplacement;

import org.evomaster.client.java.instrumentation.example.redis.RedisHashOperations;

import java.lang.reflect.Method;

public class RedisHashOperationsImpl implements RedisHashOperations {

    private static Object redisTemplate;

    public static void setRedisTemplate(Object template) {
        redisTemplate = template;
    }

    @Override
    public String getValue(String key, String hashKey) {
        try {
            Method opsForHash = redisTemplate.getClass().getMethod("opsForHash");
            Object ops = opsForHash.invoke(redisTemplate);
            Method get = ops.getClass().getMethod("get", Object.class, Object.class);
            get.setAccessible(true);
            return (String) get.invoke(ops, key, hashKey);
        } catch (Exception e) {
            throw new RuntimeException("Reflection call failed", e);
        }
    }
}