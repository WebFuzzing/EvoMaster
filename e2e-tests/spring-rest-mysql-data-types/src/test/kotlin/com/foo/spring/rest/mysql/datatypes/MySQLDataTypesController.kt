package com.foo.spring.rest.mysql.datatypes

class MySQLDataTypesController : SpringRestMySqlController(MySQLDataTypesApp::class.java) {
    override fun pathToFlywayFiles() = "classpath:/schema/mysql"
}