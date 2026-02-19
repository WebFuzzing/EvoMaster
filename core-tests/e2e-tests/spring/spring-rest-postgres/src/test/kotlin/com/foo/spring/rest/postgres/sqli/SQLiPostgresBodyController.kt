package com.foo.spring.rest.postgres.sqli

import com.foo.spring.rest.postgres.SpringRestPostgresController
import com.foo.spring.rest.postgres.sqli.body.BodySQLiApplication
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.DbSpecification

class SQLiPostgresBodyController : SpringRestPostgresController(BodySQLiApplication::class.java){
    override fun pathToFlywayFiles() = "classpath:/schema/sqli"
    override fun getDbSpecifications(): MutableList<DbSpecification>? = mutableListOf(
        DbSpecification(
            DatabaseType.POSTGRES,
            sqlConnection
        ).withSchemas("public").withInitSqlOnResourcePath("/schema/sqli/data.sql"))
}
