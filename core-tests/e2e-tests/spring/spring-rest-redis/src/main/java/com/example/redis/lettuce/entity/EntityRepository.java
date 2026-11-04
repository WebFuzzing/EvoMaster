package com.example.redis.lettuce.entity;

import org.springframework.data.repository.CrudRepository;

public interface EntityRepository extends CrudRepository<Entity, String> {
}
