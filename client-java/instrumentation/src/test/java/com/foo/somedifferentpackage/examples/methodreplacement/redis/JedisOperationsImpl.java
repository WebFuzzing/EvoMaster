package com.foo.somedifferentpackage.examples.methodreplacement.redis;

import org.evomaster.client.java.instrumentation.example.redis.JedisOperations;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.UnifiedJedis;
import redis.clients.jedis.search.FTCreateParams;
import redis.clients.jedis.search.schemafields.SchemaField;
import redis.clients.jedis.search.schemafields.TextField;

import java.util.Collections;

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
    public void ftCreate(String index, String prefix, String textField) {
        FTCreateParams params = FTCreateParams.createParams().prefix(prefix);
        jedis.ftCreate(index, params, Collections.<SchemaField>singletonList(TextField.of(textField)));
    }

    @Override
    public void hset(String key, String field, String value) {
        jedis.hset(key, field, value);
    }

    @Override
    public long ftSearch(String index, String query) {
        return jedis.ftSearch(index, query).getTotalResults();
    }
}
