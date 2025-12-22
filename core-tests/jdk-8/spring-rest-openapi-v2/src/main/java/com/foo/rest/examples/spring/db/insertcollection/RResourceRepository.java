package com.foo.rest.examples.spring.db.insertcollection;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * created by manzhang on 2021/11/10
 */
@Repository
public interface RResourceRepository extends CrudRepository<RResourceEntity, Long> {
}
