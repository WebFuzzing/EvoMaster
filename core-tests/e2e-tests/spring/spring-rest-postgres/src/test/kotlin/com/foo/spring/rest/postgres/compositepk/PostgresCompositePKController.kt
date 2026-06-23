package com.foo.spring.rest.postgres.compositepk

import com.foo.spring.rest.postgres.SpringRestPostgresController

class PostgresCompositePKController : SpringRestPostgresController(PostgresCompositePKApplication::class.java) {

    override fun pathToFlywayFiles() = "classpath:/schema/compositepk"
}
