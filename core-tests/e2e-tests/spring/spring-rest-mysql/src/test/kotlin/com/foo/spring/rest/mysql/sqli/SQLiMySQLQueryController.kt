package com.foo.spring.rest.mysql.sqli

import com.foo.spring.rest.mysql.SpringRestMySqlController
import com.foo.spring.rest.mysql.sqli.query.QuerySQLiApplication
import org.evomaster.client.java.sql.DbSpecification

class SQLiMySQLQueryController : SpringRestMySqlController(QuerySQLiApplication::class.java){
    override fun pathToFlywayFiles() = "classpath:/schema/sqli"
    override fun getDbSpecifications(): MutableList<DbSpecification>? {
        return null
    }
}
