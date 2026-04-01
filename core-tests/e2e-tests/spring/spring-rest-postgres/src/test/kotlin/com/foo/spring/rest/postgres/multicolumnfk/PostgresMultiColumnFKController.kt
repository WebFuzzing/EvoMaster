package com.foo.spring.rest.postgres.multicolumnfk

import com.foo.spring.rest.postgres.SpringRestPostgresController

class PostgresMultiColumnFKController : SpringRestPostgresController(PostgresMultiColumnFKApplication::class.java) {

    override fun pathToFlywayFiles() = "classpath:/schema/multicolumnfk"
}
