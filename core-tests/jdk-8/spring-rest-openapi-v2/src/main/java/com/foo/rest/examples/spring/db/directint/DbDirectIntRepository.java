package com.foo.rest.examples.spring.db.directint;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DbDirectIntRepository extends CrudRepository<DbDirectIntEntity, Long> {

    List<DbDirectIntEntity> findByXIsAndYIs(Integer x, Integer y);
}
