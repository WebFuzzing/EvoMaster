package com.foo.rest.examples.spring.db.auth.db;

import org.springframework.data.repository.CrudRepository;

public interface AuthUserRepository extends CrudRepository<AuthUserEntity, String> {
}
