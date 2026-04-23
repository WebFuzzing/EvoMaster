package com.foo.spring.rest.postgres.ind0

import com.foo.spring.rest.postgres.SpringRestPostgresController

/**
 * Created by arcuri82 on 21-Jun-19.
 */
class Ind0Controller : SpringRestPostgresController(Ind0App::class.java) {

    override fun pathToFlywayFiles() = "classpath:/schema/ind0"
}