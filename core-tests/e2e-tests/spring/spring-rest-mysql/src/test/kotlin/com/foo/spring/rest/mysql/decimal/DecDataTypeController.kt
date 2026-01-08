package com.foo.spring.rest.mysql.decimal

import com.foo.spring.rest.mysql.SpringRestMySqlController

class DecDataTypeController : SpringRestMySqlController(DecDataTypeApp::class.java) {
    override fun pathToFlywayFiles() = "classpath:/schema/decimal"
}