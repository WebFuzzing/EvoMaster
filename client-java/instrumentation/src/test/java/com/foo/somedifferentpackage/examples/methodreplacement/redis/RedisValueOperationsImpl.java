package com.foo.somedifferentpackage.examples.methodreplacement.redis;

import org.evomaster.client.java.instrumentation.example.redis.RedisValueOperations;

import java.lang.reflect.Method;

public class RedisValueOperationsImpl implements RedisValueOperations {

    private static Object redisTemplate;

    public static void setRedisTemplate(Object template) {
        redisTemplate = template;
    }

    @Override
    public String getValue(String key) {
        try {
            Method opsForValue = redisTemplate.getClass().getMethod("opsForValue");
            Object ops = opsForValue.invoke(redisTemplate);
            Method get = ops.getClass().getMethod("get", Object.class);
            get.setAccessible(true);
            return (String) get.invoke(ops, key);
        } catch (Exception e) {
            throw new RuntimeException("Reflection call failed", e);
        }
    }
}