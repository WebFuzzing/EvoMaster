package com.foo.spring.rest.mysql.datatypes

import com.foo.spring.rest.mysql.SpringRestMySqlController

class MySQLDataTypesController : SpringRestMySqlController(MySQLDataTypesApp::class.java) {
    override fun pathToFlywayFiles() = "classpath:/schema/datatypes"
}