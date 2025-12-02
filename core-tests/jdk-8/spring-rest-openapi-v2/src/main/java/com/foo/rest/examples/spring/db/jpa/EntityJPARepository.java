package com.foo.rest.examples.spring.db.jpa;


import org.springframework.data.repository.CrudRepository;

public interface EntityJPARepository extends CrudRepository<EntityJPAData, Integer> {
}
