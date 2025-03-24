package com.foo.spring.rest.mysql.columntypes

class MySQLColumnTypesController : SpringRestMySqlController(MySQLColumnTypesApplication::class.java) {
    override fun pathToFlywayFiles() = "classpath:/schema/columntypes"
}
