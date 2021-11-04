package com.foo.spring.rest.mysql

import com.p6spy.engine.spy.P6SpyDriver
import org.evomaster.client.java.controller.EmbeddedSutController
import org.evomaster.client.java.controller.api.dto.AuthenticationDto
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.db.DbCleaner
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem
import org.hibernate.dialect.MySQL8Dialect
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.containers.GenericContainer
import java.sql.Connection
import java.sql.DriverManager

abstract class SpringRestMySqlController (
    private val applicationClass: Class<*>
) : EmbeddedSutController() {

    private val MYSQL_DB_NAME = "test"
    private val MYSQL_PORT = 3306

    private var ctx: ConfigurableApplicationContext? = null

    private val mysql: GenericContainer<*> = GenericContainer<Nothing>("mysql:8.0.23")
        .apply { withEnv(object : HashMap<String?, String?>() {
            init {
                put("MYSQL_ROOT_PASSWORD", "root")
                put("MYSQL_DATABASE", MYSQL_DB_NAME)
                put("MYSQL_USER", "test")
                put("MYSQL_PASSWORD", "test")
            }
        }) }
        .apply { withExposedPorts(MYSQL_PORT) }


    private var connection: Connection? = null

    init {
        super.setControllerPort(0)
    }

    protected abstract fun pathToFlywayFiles() : String

    override fun startSut(): String {
        mysql.start()

        val host = mysql.getContainerIpAddress()
        val port = mysql.getMappedPort(MYSQL_PORT)
        val url = "jdbc:p6spy:mysql://$host:$port/$MYSQL_DB_NAME"

        connection = DriverManager.getConnection(url, "test", "test")


        ctx = SpringApplication.run(applicationClass,
            "--server.port=0",
            "--spring.datasource.url=$url",
            "--spring.datasource.driver-class-name=" + P6SpyDriver::class.java.name,
            "--spring.jpa.database-platform="+ MySQL8Dialect::class.java.name,
            "--spring.datasource.username=test",
            "--spring.datasource.password=test",
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
        mysql.stop()
    }

    override fun getPackagePrefixesToCover(): String {
        return "com.foo."
    }

    override fun resetStateOfSUT() {
        DbCleaner.clearDatabase(
            connection,
            MYSQL_DB_NAME,
            listOf(),
            DatabaseType.MYSQL
        )
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
        return "com.mysql.cj.jdbc.Driver"
    }

    override fun getPreferredOutputFormat(): SutInfoDto.OutputFormat {
        return SutInfoDto.OutputFormat.KOTLIN_JUNIT_5
    }
}