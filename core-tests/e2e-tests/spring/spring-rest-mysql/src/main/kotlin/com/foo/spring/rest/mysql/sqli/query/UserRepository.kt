package com.foo.spring.rest.mysql.sqli.query

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


@Repository
interface UserRepository : JpaRepository<UserEntity, Long> {

}
