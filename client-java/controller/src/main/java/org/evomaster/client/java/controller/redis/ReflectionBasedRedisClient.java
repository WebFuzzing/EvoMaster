package org.evomaster.client.java.controller.redis;

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

    /**
     * Lettuce has no support for the RediSearch module, so a second, optional connection is kept
     * using Jedis (which does support it) purely to run RediSearch introspection commands such as
     * FT.INFO. It is null whenever Jedis is not present on the driver's classpath, in which case
     * RediSearch-dependent lookups degrade to empty results instead of failing the whole client.
     */
    private final Object jedisClient;        // redis.clients.jedis.UnifiedJedis

    private static final String CLOSE_METHOD = "close";
    private static final String CONNECT_METHOD = "connect";
    private static final String CREATE_METHOD = "create";
    private static final String FLUSHALL_METHOD = "flushall";
    private static final String FT_INFO_METHOD = "ftInfo";
    private static final String GET_METHOD = "get";
    private static final String HGET_METHOD = "hget";
    private static final String HGETALL_METHOD = "hgetall";
    private static final String HSET_METHOD = "hset";
    private static final String INDEX_DEFINITION_KEY = "index_definition";
    private static final String KEYS_METHOD = "keys";
    private static final String PREFIXES_KEY = "prefixes";
    private static final String SADD_METHOD = "sadd";
    private static final String SELECT_METHOD = "select";
    private static final String SET_METHOD = "set";
    private static final String SHUTDOWN_METHOD = "shutdown";
    private static final String SMEMBERS_METHOD = "smembers";
    private static final String SYNC_METHOD = "sync";
    private static final String TYPE_METHOD = "type";

    /**
     * Creates the Redis connection.
     * @param host Redis database host.
     * @param port Redis database port.
     * @param keyspace Logical database index. Default is 0.
     */
    public ReflectionBasedRedisClient(String host, int port, int keyspace) {
        try {
            Class<?> redisClientClass = Class.forName("io.lettuce.core.RedisClient");
            Class<?> redisURIClass = Class.forName("io.lettuce.core.RedisURI");

            Method createUri = redisURIClass.getMethod(CREATE_METHOD, String.class);
            Object uri = createUri.invoke(null, "redis://" + host + ":" + port + "/" + keyspace);

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

        this.jedisClient = createJedisClient(host, port);
    }

    /**
     * Best-effort creation of a Jedis-based connection, used only for RediSearch introspection.
     * Unlike the Lettuce connection above, its absence does not prevent the rest of this client
     * from working, since plain Redis heuristics have no need for it.
     */
    private static Object createJedisClient(String host, int port) {
        try {
            Class<?> hostAndPortClass = Class.forName("redis.clients.jedis.HostAndPort");
            Object hostAndPort = hostAndPortClass.getConstructor(String.class, int.class).newInstance(host, port);

            Class<?> unifiedJedisClass = Class.forName("redis.clients.jedis.UnifiedJedis");
            return unifiedJedisClass.getConstructor(hostAndPortClass).newInstance(hostAndPort);
        } catch (Exception e) {
            return null;
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

        try {
            if (jedisClient != null) {
                Method close = jedisClient.getClass().getMethod(CLOSE_METHOD);
                close.invoke(jedisClient);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Returns the key prefixes declared for a RediSearch index via FT.CREATE, as reported live by
     * FT.INFO. An index may declare more than one prefix; a document is a candidate for the index
     * if its key matches any of them.
     *
     * @param indexName the name of the RediSearch index
     * @return the declared prefixes, or an empty list if the index does not exist, the RediSearch
     * module is not loaded, or Jedis is not available on the driver's classpath
     */
    public List<String> getIndexPrefixes(String indexName) {
        if (jedisClient == null) {
            return Collections.emptyList();
        }
        try {
            Method ftInfo = jedisClient.getClass().getMethod(FT_INFO_METHOD, String.class);
            Map<?, ?> info = (Map<?, ?>) ftInfo.invoke(jedisClient, indexName);
            return extractPrefixes(info);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /**
     * The "index_definition" entry of an FT.INFO reply carries the declared prefixes, but Jedis may
     * surface it either as a nested Map (RESP3) or as a flat list alternating field names and values
     * (RESP2), depending on the protocol negotiated with the server.
     */
    private static List<String> extractPrefixes(Map<?, ?> info) {
        if (info == null) {
            return Collections.emptyList();
        }

        Object definition = info.get(INDEX_DEFINITION_KEY);

        if (definition instanceof Map) {
            return toStringList(((Map<?, ?>) definition).get(PREFIXES_KEY));
        }

        if (definition instanceof List) {
            List<?> flatDefinition = (List<?>) definition;
            for (int i = 0; i + 1 < flatDefinition.size(); i += 2) {
                if (PREFIXES_KEY.equals(String.valueOf(flatDefinition.get(i)))) {
                    return toStringList(flatDefinition.get(i + 1));
                }
            }
        }

        return Collections.emptyList();
    }

    private static List<String> toStringList(Object value) {
        if (!(value instanceof Collection)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        for (Object element : (Collection<?>) value) {
            result.add(String.valueOf(element));
        }
        return result;
    }

    /** Selects the logical database for subsequent commands on this connection.
     * <p>Redis supports multiple logical databases identified by a zero-based integer index,
     * referred to as a keyspace. All keys are scoped to the selected keyspace, meaning that
     * the same key can exist independently in different keyspaces. The default keyspace is 0.
     *
     * @param keyspace the zero-based index of the logical database to select
     */
    public void select(int keyspace) {
        invoke(SELECT_METHOD, keyspace);
    }

    /** Equivalent to SET key value */
    public void setValue(String key, String value) {
        invoke(SET_METHOD, key, value);
    }

    /** Equivalent to GET key */
    public String getValue(String key) {
        return (String) invoke(GET_METHOD, key);
    }

    /** Equivalent to HGET key field */
    public String getHashValue(String key, String field) {
        return (String) invoke(HGET_METHOD, key, field);
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

    /** SADD key */
    public void addMember(String key, String member) {
        invoke(SADD_METHOD, key, new String[]{member});
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