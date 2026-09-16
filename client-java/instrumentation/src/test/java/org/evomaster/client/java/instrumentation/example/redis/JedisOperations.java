package org.evomaster.client.java.instrumentation.example.redis;

/**
 * Minimal Jedis-backed Redis client used to verify method-replacement instrumentation
 * on {@link org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses.ConnectionClassReplacement}.
 */
public interface JedisOperations {
    String get(String key);
    void ftCreate(String index, String prefix, String textField);
    void hset(String key, String field, String value);
    long ftSearch(String index, String query);
}
