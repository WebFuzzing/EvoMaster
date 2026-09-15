package org.evomaster.client.java.instrumentation.example.redis;

public interface JedisOperations {
    String get(String key);
    Object jsonGet(String key);
    void jsonSet(String key, Object value);
}
