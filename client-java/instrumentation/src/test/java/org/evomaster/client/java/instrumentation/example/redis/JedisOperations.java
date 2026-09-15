package org.evomaster.client.java.instrumentation.example.redis;

/**
 * Minimal Jedis-backed Redis client used to verify method-replacement instrumentation
 * on {@link org.evomaster.client.java.instrumentation.coverage.methodreplacement.thirdpartyclasses.ConnectionClassReplacement}.
 */
public interface JedisOperations {
    String get(String key);
    Object jsonGet(String key);
    void jsonSet(String key, Object value);
}
