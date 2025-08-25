package org.evomaster.client.java.instrumentation.example.redis;

public interface RedisOperations {
    String get(String key);
    String hget(String key, String hash);
}
