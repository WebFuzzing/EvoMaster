package com.foo.spring.rest.postgres.sqli

import com.foo.spring.rest.postgres.SpringRestPostgresController
import com.foo.spring.rest.postgres.sqli.query.QuerySQLiApplication
import org.evomaster.client.java.sql.DbSpecification

class SQLiPostgresQueryController : SpringRestPostgresController(QuerySQLiApplication::class.java){
    override fun pathToFlywayFiles() = "classpath:/schema/sqli"
    override fun resetStateOfSUT() {
        QuerySQLiApplication.reset()
    }
}
