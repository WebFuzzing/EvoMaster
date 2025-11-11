package com.foo.rest.examples.spring.adaptivehypermutation.entity;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/** automatically created on 2020-10-22 */
@Repository
public interface FooRepository extends CrudRepository<FooEntity, Integer> { }
