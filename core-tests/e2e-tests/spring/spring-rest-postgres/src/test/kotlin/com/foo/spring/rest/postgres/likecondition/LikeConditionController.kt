package com.foo.spring.rest.postgres.likecondition

import com.foo.spring.rest.postgres.SpringRestPostgresController

class LikeConditionController : SpringRestPostgresController(LikeConditionApp::class.java) {
    override fun pathToFlywayFiles() = "classpath:/schema/likecondition"
}