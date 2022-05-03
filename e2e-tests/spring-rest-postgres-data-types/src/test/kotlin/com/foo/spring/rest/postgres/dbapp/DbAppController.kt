package com.foo.spring.rest.postgres.dbapp

import com.foo.spring.rest.postgres.SpringRestPostgresController

/**
 * Created by arcuri82 on 21-Jun-19.
 */
class DbAppController : SpringRestPostgresController(DbApp::class.java) {

    override fun pathToFlywayFiles() = "classpath:/schema/postgresdb"
}