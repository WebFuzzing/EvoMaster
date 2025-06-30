package org.evomaster.client.java.instrumentation.example.redis;

import java.util.Map;

public interface RedisCommandsOperations {
    String get(String key);
    String hget(String key, String hashKey);
    Map<String,String> hgetall(String key);
}
