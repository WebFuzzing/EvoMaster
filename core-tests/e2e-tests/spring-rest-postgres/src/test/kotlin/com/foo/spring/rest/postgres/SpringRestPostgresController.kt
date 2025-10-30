package com.foo.spring.rest.postgres

import org.evomaster.client.java.controller.EmbeddedSutController
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.DbSpecification
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.containers.GenericContainer
import java.sql.Connection

/**
 * Created by arcuri82 on 21-Jun-19.
 */
abstract class SpringRestPostgresController(
        protected val applicationClass: Class<*>
) : EmbeddedSutController() {

    protected var ctx: ConfigurableApplicationContext? = null

    private val POSTGRES_VERSION:String = "14";

    private val postgres : GenericContainer<*> = GenericContainer<Nothing>("postgres:$POSTGRES_VERSION" )
            .apply{withExposedPorts(5432)}
            .apply{withEnv("POSTGRES_HOST_AUTH_METHOD","trust")}


    private var sqlConnection: Connection? = null


    init {
        super.setControllerPort(0)
    }

    /**
     * As we need to handle the low level details of Postgres, we are going to
     * create the schema directly with Flyway, and not delegating it to Hibernate
     */
    protected abstract fun pathToFlywayFiles() : String


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
                "--spring.jpa.hibernate.ddl-auto=validate",
                "--spring.flyway.locations=${pathToFlywayFiles()}",
                "--spring.jmx.enabled=false"
        )!!


        sqlConnection?.close()

        val jdbc = ctx!!.getBean(JdbcTemplate::class.java)
        sqlConnection = jdbc.dataSource!!.connection

        return "http://localhost:" + getSutPort()
    }

    protected fun getSutPort(): Int {
        return (ctx!!.environment
                .propertySources.get("server.ports")!!
                .source as Map<*, *>)["local.server.port"] as Int
    }


    override fun isSutRunning(): Boolean {
        return ctx != null && ctx!!.isRunning
    }

    override fun stopSut() {
        ctx?.stop()
        ctx?.close()
        postgres.stop()
    }

    override fun getPackagePrefixesToCover(): String {
        return "com.foo."
    }

    override fun resetStateOfSUT() {
//        DbCleaner.clearDatabase_Postgres(sqlConnection, "public", listOf("flyway_schema_history"))
    }

    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
                "http://localhost:" + getSutPort() + "/v2/api-docs",
                null
        )
    }

    override fun getInfoForAuthentication(): List<AuthenticationDto>? {
        return null
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
