package com.foo.rest.examples.bb.sqli

import com.foo.rest.examples.bb.SpringController
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.DbSpecification
import org.springframework.boot.SpringApplication
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.containers.GenericContainer
import java.sql.Connection


class BBSQLiController : SpringController(BBQuerySQLiApplication::class.java){

    private val POSTGRES_VERSION:String = "14";

    private val postgres : GenericContainer<*> = GenericContainer<Nothing>("postgres:$POSTGRES_VERSION" )
        .apply{withExposedPorts(5432)}
        .apply{withEnv("POSTGRES_HOST_AUTH_METHOD","trust")}


    private var sqlConnection: Connection? = null


    init {
        super.setControllerPort(0)
    }

    fun pathToFlywayFiles() = "classpath:/schema/sqli"

    override fun startSut(): String {

        postgres.start()
        val host = postgres.getContainerIpAddress()
        val port = postgres.getMappedPort(5432)
        val dbUrl = "jdbc:postgresql://$host:$port/postgres"

        ctx = SpringApplication.run(applicationClass,
            "--server.port=0",
            "--spring.datasource.url=$dbUrl",
            "--spring.jpa.database=postgresql",
            "--spring.datasource.username=postgres",
            "--spring.datasource.password",
            "--spring.jpa.properties.hibernate.show_sql=true",
            "--spring.jpa.hibernate.ddl-auto=create-drop",
            "--spring.flyway.locations=${pathToFlywayFiles()}",
            "--spring.jmx.enabled=false"
        )!!


        sqlConnection?.close()

        val jdbc = ctx!!.getBean(JdbcTemplate::class.java)
        sqlConnection = jdbc.dataSource!!.connection

        return "http://localhost:" + sutPort
    }


    override fun stopSut() {
        ctx?.stop()
        ctx?.close()
        postgres.stop()
    }

    override fun getPackagePrefixesToCover(): String {
        return "com.foo."
    }

    override fun getDbSpecifications(): MutableList<DbSpecification>? = mutableListOf(
        DbSpecification(
            DatabaseType.POSTGRES,
            sqlConnection
        ).withSchemas("public"))

    override fun getPreferredOutputFormat(): SutInfoDto.OutputFormat {
        return SutInfoDto.OutputFormat.KOTLIN_JUNIT_5
    }
}