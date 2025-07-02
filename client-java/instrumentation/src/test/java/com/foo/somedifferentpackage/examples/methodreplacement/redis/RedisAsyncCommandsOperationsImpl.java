package com.foo.somedifferentpackage.examples.methodreplacement.redis;

import org.evomaster.client.java.instrumentation.example.redis.RedisAsyncCommandsOperations;

import java.lang.reflect.Method;

public class RedisAsyncCommandsOperationsImpl implements RedisAsyncCommandsOperations {

    private static Object asyncCommands;

    private static final String GET = "get";
    private static final String HGET = "hget";
    private static final String HGETALL = "hgetall";

    public static void setAsyncCommands(Object commands) {
        asyncCommands = commands;
    }

    @Override
    public Object get(String key) {
        try {
            Method method = asyncCommands.getClass().getMethod(GET, Object.class);
            return method.invoke(asyncCommands, key);
        } catch (Exception e) {
            throw new RuntimeException("Reflection for async GET failed", e);
        }
    }

    @Override
    public Object hget(String key, String hashKey) {
        try {
            Method method = asyncCommands.getClass().getMethod(HGET, Object.class, Object.class);
            return method.invoke(asyncCommands, key, hashKey);
        } catch (Exception e) {
            throw new RuntimeException("Reflection for async HGET failed", e);
        }
    }

    @Override
    public Object hgetall(String key) {
        try {
            Method method = asyncCommands.getClass().getMethod(HGETALL, Object.class);
            return method.invoke(asyncCommands, key);
        } catch (Exception e) {
            throw new RuntimeException("Reflection for async HGETALL failed", e);
        }
    }
}