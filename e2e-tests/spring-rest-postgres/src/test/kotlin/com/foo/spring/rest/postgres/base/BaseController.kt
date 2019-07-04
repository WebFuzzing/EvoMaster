package com.foo.spring.rest.postgres.base


import com.foo.spring.rest.postgres.SpringRestPostgresController

/**
 * Created by arcuri82 on 21-Jun-19.
 */
class BaseController : SpringRestPostgresController(BaseApp::class.java) {

    override fun pathToFlywayFiles() = "classpath:/schema/base"
}