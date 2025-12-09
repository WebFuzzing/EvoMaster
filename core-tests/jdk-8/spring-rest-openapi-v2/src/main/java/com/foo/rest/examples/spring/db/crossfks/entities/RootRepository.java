package com.foo.rest.examples.spring.db.crossfks.entities;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface RootRepository extends CrudRepository<RootTableEntity, Long> {

    RootTableEntity findByName(String name);

}

