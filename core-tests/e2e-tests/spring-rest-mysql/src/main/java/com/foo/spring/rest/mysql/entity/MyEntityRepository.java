package com.foo.spring.rest.mysql.entity;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MyEntityRepository extends JpaRepository<MyEntity, Long> {
}
