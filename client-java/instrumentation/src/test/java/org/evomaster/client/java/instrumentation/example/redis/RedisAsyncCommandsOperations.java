package org.evomaster.client.java.instrumentation.example.redis;

public interface RedisAsyncCommandsOperations {
    Object get(String key);
    Object hget(String key, String hashKey);
    Object hgetall(String key);
}
