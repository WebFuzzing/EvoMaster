package com.foo.spring.rest.postgres.basic

import com.foo.spring.rest.postgres.SpringRestPostgresController

/**
 * Created by arcuri82 on 21-Jun-19.
 */
class BasicController : SpringRestPostgresController(BasicApp::class.java) {

    override fun pathToFlywayFiles() = "classpath:/schema/basic"
}