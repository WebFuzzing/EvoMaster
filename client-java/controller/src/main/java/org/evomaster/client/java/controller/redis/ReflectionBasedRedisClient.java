package org.evomaster.client.java.controller.redis;

import org.evomaster.client.java.utils.SimpleLogger;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RedisClient that uses Lettuce dynamically via reflection, avoiding
 * compile-time dependency on Spring or Lettuce.
 */
public class ReflectionBasedRedisClient {

    private final Object lettuceClient;      // io.lettuce.core.RedisClient
    private final Object connection;       // io.lettuce.core.api.StatefulRedisConnection
    private final Object syncCommands;     // io.lettuce.core.api.sync.RedisCommands

    private static final String CLOSE_METHOD = "close";
    private static final String CONNECT_METHOD = "connect";
    private static final String CREATE_METHOD = "create";
    private static final String FLUSHALL_METHOD = "flushall";
    private static final String GET_METHOD = "get";
    private static final String HGETALL_METHOD = "hgetall";
    private static final String HSET_METHOD = "hset";
    private static final String KEYS_METHOD = "keys";
    private static final String SET_METHOD = "set";
    private static final String SHUTDOWN_METHOD = "shutdown";
    private static final String SMEMBERS_METHOD = "smembers";
    private static final String SYNC_METHOD = "sync";
    private static final String TYPE_METHOD = "type";

    public ReflectionBasedRedisClient(String host, int port) {
        try {
            Class<?> redisClientClass = Class.forName("io.lettuce.core.RedisClient");
            Class<?> redisURIClass = Class.forName("io.lettuce.core.RedisURI");

            Method createUri = redisURIClass.getMethod(CREATE_METHOD, String.class);
            Object uri = createUri.invoke(null, "redis://" + host + ":" + port);

            SimpleLogger.debug("Connecting to Redis with PORT: " + port);

            Method createClient = redisClientClass.getMethod(CREATE_METHOD, redisURIClass);
            this.lettuceClient = createClient.invoke(null, uri);

            Method connectMethod = redisClientClass.getMethod(CONNECT_METHOD);
            this.connection = connectMethod.invoke(lettuceClient);

            Class<?> statefulConnClass = Class.forName("io.lettuce.core.api.StatefulRedisConnection");
            Method syncMethod = statefulConnClass.getMethod(SYNC_METHOD);
            this.syncCommands = syncMethod.invoke(connection);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Lettuce Redis client via reflection", e);
        }
    }

    public void close() {
        try {
            if (connection != null) {
                Method close = connection.getClass().getMethod(CLOSE_METHOD);
                close.invoke(connection);
            }
            if (lettuceClient != null) {
                Method shutdown = lettuceClient.getClass().getMethod(SHUTDOWN_METHOD);
                shutdown.invoke(lettuceClient);
            }
        } catch (Exception ignored) {}
    }

    /** Equivalent to SET key value */
    public void setValue(String key, String value) {
        invoke(SET_METHOD, key, value);
    }

    /** Equivalent to GET key */
    public String getValue(String key) {
        return (String) invoke(GET_METHOD, key);
    }

    /** Equivalent to KEYS * */
    public Set<String> getAllKeys() {
        Object result = invoke(KEYS_METHOD, "*");
        if (result instanceof Collection)
            return new HashSet<>((Collection<String>) result);
        return Collections.emptySet();
    }

    /** Equivalent to TYPE key */
    public String getType(String key) {
        Object result = invoke(TYPE_METHOD, key);
        return result != null ? result.toString() : null;
    }

    /** HSET key field value */
    public void hashSet(String key, String field, String value) {
        invoke(HSET_METHOD, key, field, value);
    }

    /** SMEMBERS key */
    public Set<String> getSetMembers(String key) {
        Object result = invoke(SMEMBERS_METHOD, key);
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
        invoke(FLUSHALL_METHOD);
    }

    public Map<String, String> getHashFields(String key) {
        Object result = invoke(HGETALL_METHOD, key);
        return (Map<String, String>) result;
    }
}