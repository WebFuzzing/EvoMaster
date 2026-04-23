package com.foo.spring.rest.postgres.json

import com.foo.spring.rest.postgres.SpringRestPostgresController

/**
 * Created by jgaleotti on 3-Sept-22.
 */
class JsonColumnController : SpringRestPostgresController(JsonColumnApp::class.java) {

    override fun pathToFlywayFiles() = "classpath:/schema/json"
}