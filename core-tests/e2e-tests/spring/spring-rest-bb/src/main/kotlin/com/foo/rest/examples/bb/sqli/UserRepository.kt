package com.foo.rest.examples.bb.sqli

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


@Repository
interface UserRepository : JpaRepository<UserEntity, Long> {

}
