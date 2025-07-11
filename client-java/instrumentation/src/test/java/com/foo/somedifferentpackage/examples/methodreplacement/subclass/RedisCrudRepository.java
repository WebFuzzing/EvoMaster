package com.foo.somedifferentpackage.examples.methodreplacement.subclass;

import org.evomaster.client.java.instrumentation.example.redis.RedisEntity;
import org.springframework.data.repository.CrudRepository;

public interface RedisCrudRepository extends CrudRepository<RedisEntity, String> {
}