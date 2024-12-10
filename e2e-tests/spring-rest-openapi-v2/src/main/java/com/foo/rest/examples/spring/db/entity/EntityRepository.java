package com.foo.rest.examples.spring.db.entity;


import org.springframework.data.repository.CrudRepository;

public interface EntityRepository extends CrudRepository<EntityData, Integer> {
}
