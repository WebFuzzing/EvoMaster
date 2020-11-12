package com.foo.rest.examples.spring.db.existingdata;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Created by arcuri82 on 19-Jun-19.
 */
@Repository
public interface ExistingDataRepositoryX extends CrudRepository<ExistingDataEntityX, Long> {
}
