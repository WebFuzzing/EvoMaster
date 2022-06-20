package com.foo.rest.examples.spring.db.disablesql;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DisableSqlRepository extends CrudRepository<DisableSqlEntity, Long> {

    List<DisableSqlEntity> findByIntValue(Integer intValue);

}
