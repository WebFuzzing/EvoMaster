package com.foo.spring.rest.mysql.basic

import com.foo.spring.rest.mysql.SpringRestMySqlController

class BasicController : SpringRestMySqlController(BasicApp::class.java) {
    override fun pathToFlywayFiles() = "classpath:/schema/basic"
}