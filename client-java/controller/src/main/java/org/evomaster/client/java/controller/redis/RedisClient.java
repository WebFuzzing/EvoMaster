package org.evomaster.client.java.controller.redis;

import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RedisClient that uses Lettuce dynamically via reflection, avoiding
 * compile-time dependency on Spring or Lettuce.
 */
public class RedisClient {

    private final Object redisClient;      // io.lettuce.core.RedisClient
    private final Object connection;       // io.lettuce.core.api.StatefulRedisConnection
    private final Object syncCommands;     // io.lettuce.core.api.sync.RedisCommands

    public RedisClient(String host, int port) {
        try {
            Class<?> redisClientClass = Class.forName("io.lettuce.core.RedisClient");
            Class<?> redisURIClass = Class.forName("io.lettuce.core.RedisURI");

            Method createUri = redisURIClass.getMethod("create", String.class);
            Object uri = createUri.invoke(null, "redis://" + host + ":" + port);

            SimpleLogger.debug("Connecting to Redis with PORT: " + port);

            Method createClient = redisClientClass.getMethod("create", redisURIClass);
            this.redisClient = createClient.invoke(null, uri);

            Method connectMethod = redisClientClass.getMethod("connect");
            this.connection = connectMethod.invoke(redisClient);

            Class<?> statefulConnClass = Class.forName("io.lettuce.core.api.StatefulRedisConnection");
            Method syncMethod = statefulConnClass.getMethod("sync");
            this.syncCommands = syncMethod.invoke(connection);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Lettuce Redis client via reflection", e);
        }
    }

    public void close() {
        try {
            if (connection != null) {
                Method close = connection.getClass().getMethod("close");
                close.invoke(connection);
            }
            if (redisClient != null) {
                Method shutdown = redisClient.getClass().getMethod("shutdown");
                shutdown.invoke(redisClient);
            }
        } catch (Exception ignored) {}
    }

    /** Equivalent to SET key value */
    public void setValue(String key, String value) {
        invoke("set", key, value);
    }

    /** Equivalent to GET key */
    public String getValue(String key) {
        return (String) invoke("get", key);
    }

    /** Equivalent to KEYS * */
    public Set<String> getAllKeys() {
        Object result = invoke("keys", "*");
        if (result instanceof Collection)
            return new HashSet<>((Collection<String>) result);
        return Collections.emptySet();
    }

    /** Equivalent to TYPE key */
    public String getType(String key) {
        Object result = invoke("type", key);
        return result != null ? result.toString() : null;
    }

    /** HSET key field value */
    public void hashSet(String key, String field, String value) {
        invoke("hset", key, field, value);
    }

    /** SMEMBERS key */
    public Set<String> getSetMembers(String key) {
        Object result = invoke("smembers", key);
        if (result instanceof Collection)
            return new HashSet<>((Collection<String>) result);
        return Collections.emptySet();
    }

    private Object invoke(String methodName, Object... args) {
        try {
            Class<?>[] argTypes = Arrays.stream(args)
                    .map(Object::getClass)
                    .toArray(Class<?>[]::new);

            Method method = findMethod(syncCommands.getClass(), methodName, argTypes);
            if (method == null)
                throw new RuntimeException("Method not found: " + methodName);
            return method.invoke(syncCommands, args);

        } catch (Exception e) {
            throw new RuntimeException("Error invoking Redis command: " + methodName, e);
        }
    }

    private Method findMethod(Class<?> clazz, String name, Class<?>[] argTypes) {
        for (Method m : clazz.getMethods()) {
            if (!m.getName().equals(name)) continue;
            if (m.getParameterCount() != argTypes.length) continue;
            return m;
        }
        return null;
    }

    public Set<String> getKeysByType(String expectedType) {
        return getAllKeys().stream()
                .filter(k -> expectedType.equalsIgnoreCase(getType(k)))
                .collect(Collectors.toSet());
    }

    public void flushAll() {
        invoke("flushall");
    }

    public Map<String, String> getHashFields(String key) {
        Object result = invoke("hgetall", key);
        return (Map<String, String>) result;
    }
}