package com.foo.somedifferentpackage.examples.methodreplacement.redis;

import org.evomaster.client.java.instrumentation.example.redis.RedisCommandsOperations;

import java.lang.reflect.Method;
import java.util.Map;

public class RedisCommandsOperationsImpl implements RedisCommandsOperations {

    private static Object syncCommands;

    private static final String GET = "get";
    private static final String HGET = "hget";
    private static final String HGETALL = "hgetall";

    public static void setSyncCommands(Object commands) {
        syncCommands = commands;
    }

    @Override
    public String get(String key) {
        try {
            Method method = syncCommands.getClass().getMethod(GET, Object.class);
            return (String) method.invoke(syncCommands, key);
        } catch (Exception e) {
            throw new RuntimeException("Reflection for GET failed", e);
        }
    }

    @Override
    public String hget(String key, String hashKey) {
        try {
            Method method = syncCommands.getClass().getMethod(HGET, Object.class, Object.class);
            return (String) method.invoke(syncCommands, key, hashKey);
        } catch (Exception e) {
            throw new RuntimeException("Reflection for HGET failed", e);
        }
    }

    @Override
    public Map<String, String> hgetall(String key) {
        try {
            Method method = syncCommands.getClass().getMethod(HGETALL, Object.class);
            Object result = method.invoke(syncCommands, key);
            return (Map<String, String>) result;
        } catch (Exception e) {
            throw new RuntimeException("Reflection for HGETALL failed", e);
        }
    }
}