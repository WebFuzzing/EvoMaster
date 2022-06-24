package com.foo.rest.examples.spring.db.base;


import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DbBaseRepository extends CrudRepository<DbBaseEntity, Long> {

    List<DbBaseEntity> findByName(String name);
}
