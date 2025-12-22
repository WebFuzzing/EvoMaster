package com.foo.rest.examples.spring.db.tree;

import org.springframework.data.repository.CrudRepository;

public interface DbTreeRepository extends CrudRepository<DbTreeEntity, Long> {
}
