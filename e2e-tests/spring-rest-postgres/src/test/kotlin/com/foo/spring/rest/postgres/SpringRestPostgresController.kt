package com.foo.spring.rest.postgres

import com.p6spy.engine.spy.P6SpyDriver
import org.evomaster.client.java.controller.EmbeddedSutController
import org.evomaster.client.java.controller.api.dto.AuthenticationDto
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.db.DbCleaner
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

    private val postgres : GenericContainer<*> = GenericContainer<Nothing>("postgres:9")
            .withExposedPorts(5432)

    private var connection: Connection? = null


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
        val dbUrl = "jdbc:p6spy:postgresql://$host:$port/postgres"

        ctx = SpringApplication.run(applicationClass,
                "--server.port=0",
                "--spring.datasource.url=$dbUrl",
                "--spring.datasource.driver-class-name=" + P6SpyDriver::class.java.name,
                "--spring.jpa.database=postgresql",
                "--spring.datasource.username=postgres",
                "--spring.datasource.password",
                "--spring.jpa.properties.hibernate.show_sql=true",
                "--spring.jpa.hibernate.ddl-auto=validate",
                "--spring.flyway.locations=${pathToFlywayFiles()}",
                "--spring.jmx.enabled=false"
        )!!


        connection?.close()

        val jdbc = ctx!!.getBean(JdbcTemplate::class.java)
        connection = jdbc.dataSource!!.connection

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
        DbCleaner.clearDatabase_Postgres(connection, "public", listOf("flyway_schema_history"))
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

    override fun getConnection(): Connection? {
        return connection
    }

    override fun getDatabaseDriverName(): String? {
        return "org.postgresql.Driver"
    }

    override fun getPreferredOutputFormat(): SutInfoDto.OutputFormat {
        return SutInfoDto.OutputFormat.KOTLIN_JUNIT_5
    }

}
