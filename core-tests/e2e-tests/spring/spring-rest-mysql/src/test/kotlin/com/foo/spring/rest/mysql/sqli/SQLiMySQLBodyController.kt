package com.foo.spring.rest.mysql.sqli

import com.foo.spring.rest.mysql.SpringRestMySqlController
import com.foo.spring.rest.mysql.sqli.body.BodySQLiApplication
import org.evomaster.client.java.sql.DbSpecification

class SQLiMySQLBodyController : SpringRestMySqlController(BodySQLiApplication::class.java){
    override fun pathToFlywayFiles() = "classpath:/schema/sqli"
    override fun getDbSpecifications(): MutableList<DbSpecification>? {
        return null
    }
}
