package org.evomaster.driver.multidb

import org.evomaster.client.java.controller.EmbeddedSutController
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.auth.AuthenticationDto
import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.controller.problem.ProblemInfo
import org.evomaster.client.java.controller.problem.RestProblem
import org.evomaster.client.java.sql.DbSpecification
import org.evomaster.core.sql.multidb.MultiDbUtils
import org.hibernate.dialect.H2Dialect
import org.hibernate.dialect.MySQL8Dialect
import org.hibernate.dialect.PostgreSQL82Dialect
import org.springframework.boot.SpringApplication
import org.springframework.context.ConfigurableApplicationContext
import java.sql.Connection
import kotlin.random.Random.Default.nextInt


abstract class SpringController(protected val applicationClass: Class<*>) : EmbeddedSutController() {

    companion object{
        init {
            /**
             * To avoid issues with non-determinism checks (in particular in the handling of taint-analysis),
             * we must disable the cache in H2
             */
            System.setProperty("h2.objectCache", "false")
        }
    }

    init {
        super.setControllerPort(0)
    }


    protected var sqlConnection: Connection? = null

    protected var ctx: ConfigurableApplicationContext? = null

    protected var databaseType : DatabaseType = DatabaseType.H2

    fun changeDatabaseType(t: DatabaseType){
        if(t == databaseType){
            //nothing to do
            return
        }
        //stopSut()
        databaseType = t
        //startSut()
    }

    override fun startSut(): String {

        //lot of problem if using same H2 instance. see:
        //https://github.com/h2database/h2database/issues/227
        val rand = nextInt(0, 1_000_000_000)
        val dbName = "dbtest_$rand"

        MultiDbUtils.startDatabase(databaseType)
        MultiDbUtils.resetDatabase(dbName,databaseType)
        sqlConnection = MultiDbUtils.createConnection(dbName,databaseType)
        val baseUrl = MultiDbUtils.getBaseUrl(databaseType)

        val commonSettings = arrayOf(
            "--server.port=0",
            "--spring.jpa.properties.hibernate.show_sql=true",
            "--spring.jpa.hibernate.ddl-auto=create-drop",
            "--spring.jmx.enabled=false"
        )

        ctx = when(databaseType) {
            DatabaseType.H2 -> {
                SpringApplication.run(applicationClass,
                   *commonSettings.plus(arrayOf(
                    "--spring.datasource.url=$baseUrl$dbName;DB_CLOSE_DELAY=-1;",
                    "--spring.datasource.driverClassName=org.h2.Driver",
                    "--spring.jpa.database-platform=" + H2Dialect::class.java.name,
                    "--spring.datasource.username=sa",
                    "--spring.datasource.password",
                   ))
                )
            }
            DatabaseType.MYSQL -> {
                SpringApplication.run(applicationClass,
                    *commonSettings.plus(arrayOf(
                        "--spring.datasource.url=$baseUrl$dbName",
                        "--spring.datasource.driverClassName=com.mysql.cj.jdbc.Driver",
                        "--spring.datasource.username=root",
                        "--spring.datasource.password=root",
                        "--spring.jpa.database-platform="+ MySQL8Dialect::class.java.name,
                    ))
                )
            }
            DatabaseType.POSTGRES ->{
                SpringApplication.run(applicationClass,
                    *commonSettings.plus(arrayOf(
                        "--spring.datasource.url=$baseUrl$dbName",
                        "--spring.datasource.driverClassName=org.postgresql.Driver",
                        "--spring.datasource.username=postgres",
                        "--spring.datasource.password",
                        "--spring.jpa.database-platform=" + PostgreSQL82Dialect::class.java.name
                    ))
                )
            }
            else -> throw IllegalStateException("Not supported database type: $databaseType")
        }

        return "http://localhost:$sutPort"
    }

    protected val sutPort: Int
        get() = (ctx!!.environment
                .propertySources["server.ports"].source as Map<*, *>)["local.server.port"] as Int

    override fun isSutRunning(): Boolean {
        return ctx != null && ctx!!.isRunning
    }

    override fun stopSut() {
        ctx?.stop()
        ctx?.close()
        sqlConnection?.close()
        MultiDbUtils.stopDatabase(databaseType)
    }

    override fun getPackagePrefixesToCover(): String {
        return "com.foo."
    }

    override fun resetStateOfSUT() {
        //nothing to do. SQL reset is handled automatically
    }

    override fun getProblemInfo(): ProblemInfo {
        return RestProblem(
                "http://localhost:$sutPort/v3/api-docs",
                null
        )
    }

    override fun getInfoForAuthentication(): List<AuthenticationDto> {
        return listOf()
    }

    override fun getDbSpecifications(): List<DbSpecification>? {

        if(sqlConnection == null)
            return null

        return listOf(DbSpecification(databaseType, sqlConnection))
    }


    override fun getPreferredOutputFormat(): SutInfoDto.OutputFormat {
        return SutInfoDto.OutputFormat.KOTLIN_JUNIT_5
    }

}