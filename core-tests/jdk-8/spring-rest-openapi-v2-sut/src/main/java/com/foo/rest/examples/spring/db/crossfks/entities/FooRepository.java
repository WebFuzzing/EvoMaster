package com.foo.rest.examples.spring.db.crossfks.entities;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FooRepository extends CrudRepository<FooTableEntity, Long> {

    FooTableEntity findFooTableEntitiesByRootTableEntityNameAndName(String rootTableEntity_name, String name);

}

