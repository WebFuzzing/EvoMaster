package com.foo.spring.rest.mysql.likecondition

import com.foo.spring.rest.mysql.SpringRestMySqlController

class LikeConditionController : SpringRestMySqlController(LikeConditionApp::class.java) {
    override fun pathToFlywayFiles() = "classpath:/schema/likecondition"
}