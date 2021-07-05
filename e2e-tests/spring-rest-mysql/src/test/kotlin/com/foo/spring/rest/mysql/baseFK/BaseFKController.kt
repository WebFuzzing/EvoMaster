package com.foo.spring.rest.mysql.baseFK

import com.foo.spring.rest.mysql.SpringRestMySqlController

class BaseFKController : SpringRestMySqlController(BaseFKApp::class.java) {
    override fun pathToFlywayFiles() = "classpath:/schema/baseFK"
}