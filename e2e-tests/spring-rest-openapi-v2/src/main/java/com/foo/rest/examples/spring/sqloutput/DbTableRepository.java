package com.foo.rest.examples.spring.sqloutput;


import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface DbTableRepository extends CrudRepository<DbTableEntity, Long> {

    List<DbTableEntity> findByName(String name);
}
