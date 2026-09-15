package com.foo.somedifferentpackage.examples.methodreplacement.redis;

import org.evomaster.client.java.instrumentation.example.redis.JedisOperations;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.UnifiedJedis;

/**
 * {@link JedisOperations} implementation backed by a real {@link UnifiedJedis} connection,
 * used as the instrumentation target in JedisInstrumentedTest.
 */
public class JedisOperationsImpl implements JedisOperations {

    private final UnifiedJedis jedis;

    public JedisOperationsImpl(String host, int port) {
        this.jedis = new UnifiedJedis(new HostAndPort(host, port));
    }

    @Override
    public String get(String key) {
        return jedis.get(key);
    }

    @Override
    public Object jsonGet(String key) {
        return jedis.jsonGet(key);
    }

    @Override
    public void jsonSet(String key, Object value) {
        jedis.jsonSet(key, value);
    }
}
