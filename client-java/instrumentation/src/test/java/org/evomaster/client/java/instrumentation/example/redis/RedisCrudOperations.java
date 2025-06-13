package org.evomaster.client.java.instrumentation.example.redis;

import java.util.Optional;

public interface RedisCrudOperations {
    Optional<RedisEntity> findById(String id);
}
