package com.foo.spring.rest.mysql.sqli

import com.foo.spring.rest.mysql.SpringRestMySqlController
import com.foo.spring.rest.mysql.sqli.query.QuerySQLiApplication
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.DbSpecification

class SQLiMySQLQueryController : SpringRestMySqlController(QuerySQLiApplication::class.java){
    override fun pathToFlywayFiles() = "classpath:/schema/sqli"
    override fun getDbSpecifications(): MutableList<DbSpecification>? = mutableListOf(
        DbSpecification(DatabaseType.MYSQL, dbConnection).withSchemas("test").withInitSqlOnResourcePath("/schema/sqli/data.sql"))
}
