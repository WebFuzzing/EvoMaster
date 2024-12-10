package com.foo.rest.examples.spring.db.crossfks.entities;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface BarRepository extends CrudRepository<BarTableEntity, Long> {

    BarTableEntity findBarTableEntityByRootTableEntityNameAndName(String rootTableEntity_name, String name);
}

