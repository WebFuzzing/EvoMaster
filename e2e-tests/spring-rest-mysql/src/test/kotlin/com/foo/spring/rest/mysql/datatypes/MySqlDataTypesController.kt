package com.foo.spring.rest.mysql.datatypes

import com.foo.spring.rest.mysql.SpringRestMySqlController

class MySqlDataTypesController : SpringRestMySqlController(MySqlDataTypesApp::class.java) {
    override fun pathToFlywayFiles() = "classpath:/schema/datatypes"
}