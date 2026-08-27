package com.foo.spring.rest.multidb.mongoredispostgres

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import org.evomaster.client.java.controller.EmbeddedSutController
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem
import org.evomaster.client.java.controller.redis.ReflectionBasedRedisClient
import org.evomaster.client.java.postgres.test.utils.PostgresContainerUtils
import org.evomaster.client.java.sql.DbSpecification
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.testcontainers.containers.GenericContainer
import redis.clients.jedis.Jedis
import java.sql.Connection
import java.util.Collections

open class MultiDbMongoRedisPostgresSutController : EmbeddedSutController() {

    private val postgres = PostgresContainerUtils.newContainer()

    private val mongodb = GenericContainer("mongo:6.0")
        .withTmpFs(Collections.singletonMap("/data/db", "rw"))
        .withExposedPorts(27017)

    private val redisContainer = GenericContainer("redis:7.0")
        .withExposedPorts(6379)

    private var ctx: ConfigurableApplicationContext? = null
    private var sqlConnection: Connection? = null
    private var mongoClient: MongoClient? = null
    private var redisClient: Jedis? = null
    private var reflectionRedisClient: ReflectionBasedRedisClient? = null

    private val mongoDbName = "multidb_mongo"

    init {
        super.setControllerPort(0)
    }

    override fun startSut(): String {
        postgres.start()
        val dbUrl = PostgresContainerUtils.getJdbcUrl(postgres)

        mongodb.start()
        val mongoHost = mongodb.host
        val mongoPort = mongodb.getMappedPort(27017)
        mongoClient = MongoClients.create("mongodb://$mongoHost:$mongoPort/$mongoDbName")

        redisContainer.start()
        val redisHost = redisContainer.host
        val redisPort = redisContainer.getMappedPort(6379)

        System.setProperty("spring.redis.host", redisHost)
        System.setProperty("spring.redis.port", redisPort.toString())

        redisClient = Jedis(redisHost, redisPort)
        reflectionRedisClient = ReflectionBasedRedisClient(redisHost, redisPort, 0)

        ctx = SpringApplication.run(
            MultiDbMongoRedisPostgresApp::class.java,
            "--server.port=0",
            "--spring.datasource.url=$dbUrl",
            "--spring.jpa.database=postgresql",
            "--spring.datasource.username=postgres",
            "--spring.datasource.password",
            "--spring.jpa.properties.hibernate.show_sql=true",
            "--spring.jpa.hibernate.ddl-auto=validate",
            "--spring.flyway.locations=classpath:/schema",
            "--spring.data.mongodb.host=$mongoHost",
            "--spring.data.mongodb.port=$mongoPort",
            "--spring.data.mongodb.database=$mongoDbName",
            "--spring.data.redis.host=$redisHost",
            "--spring.data.redis.port=$redisPort",
            "--spring.jmx.enabled=false"
        )!!

        sqlConnection?.close()
        val jdbc = ctx!!.getBean(JdbcTemplate::class.java)
        sqlConnection = jdbc.dataSource!!.connection

        return "http://localhost:$sutPort"
    }

    protected val sutPort: Int
        get() = (ctx!!.environment
            .propertySources["server.ports"]!!
            .source as Map<*, *>)["local.server.port"] as Int

    override fun stopSut() {
        ctx?.stop()
        ctx?.close()
        sqlConnection?.close()
        postgres.stop()
        mongodb.stop()
        redisContainer.stop()
    }

    override fun resetStateOfSUT() {
        mongoClient?.getDatabase(mongoDbName)?.drop()
        redisClient?.flushDB()
    }

    override fun isSutRunning(): Boolean {
        return ctx != null && ctx!!.isRunning
    }

    override fun getPackagePrefixesToCover(): String {
        return "com.foo."
    }

    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
            "http://localhost:$sutPort/v2/api-docs",
            null
        )
    }

    override fun getInfoForAuthentication(): List<AuthenticationDto>? {
        return null
    }

    override fun getDbSpecifications(): List<DbSpecification>? {
        return if (sqlConnection == null) null
        else listOf(
            DbSpecification(DatabaseType.POSTGRES, sqlConnection).withSchemas("public")
        )
    }

    override fun getMongoConnection(): Any? {
        return mongoClient
    }

    override fun getRedisConnection(): ReflectionBasedRedisClient? {
        return reflectionRedisClient
    }

    override fun getPreferredOutputFormat(): SutInfoDto.OutputFormat {
        return SutInfoDto.OutputFormat.KOTLIN_JUNIT_5
    }
}
