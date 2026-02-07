package com.foo.spring.rest.postgres

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.DbCleaner
import org.evomaster.client.java.sql.DbSpecification
import org.springframework.boot.SpringApplication
import org.springframework.jdbc.core.JdbcTemplate
import java.util.*

open class SpringRestPostgresSQLiController(applicationClass: Class<*>) : SpringRestPostgresController(applicationClass) {
    override fun pathToFlywayFiles() = "/schema/sqli/data.sql"

    private var dbSpecification: MutableList<DbSpecification?>? = null

    override fun startSut(): String {

        postgres.start()
        val host = postgres.getContainerIpAddress()
        val port = postgres.getMappedPort(5432)
        val dbUrl = "jdbc:postgresql://$host:$port/postgres"

        ctx = SpringApplication.run(applicationClass,
            "--server.port=0",
            "--spring.datasource.url=$dbUrl",
            "--spring.jpa.database=postgresql",
            "--spring.flyway.enabled=false",
            "--spring.datasource.username=postgres",
            "--spring.datasource.password",
            "--spring.jpa.properties.hibernate.show_sql=true",
            "--spring.jpa.hibernate.ddl-auto=create-drop",
            "--spring.jmx.enabled=false"
        )!!


        sqlConnection?.close()

        val jdbc = ctx!!.getBean(JdbcTemplate::class.java)
        sqlConnection = jdbc.dataSource!!.connection

        dbSpecification = Arrays.asList<DbSpecification?>(
            DbSpecification(DatabaseType.POSTGRES, sqlConnection)
                .withInitSqlOnResourcePath(pathToFlywayFiles())
        )
        return "http://localhost:" + getSutPort()
    }

    override fun resetStateOfSUT() {
    }

    override fun getDbSpecifications(): MutableList<DbSpecification>? = dbSpecification as MutableList<DbSpecification>?

}