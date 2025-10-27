package com.foo.spring.rest.postgres.columntypes

import com.foo.spring.rest.postgres.SpringRestPostgresController

/**
 * Created by arcuri82 on 21-Jun-19.
 */
class PostgresColumnTypesController : SpringRestPostgresController(PostgresColumnTypesApplication::class.java) {

    override fun pathToFlywayFiles() = "classpath:/schema/columntypes"
}
