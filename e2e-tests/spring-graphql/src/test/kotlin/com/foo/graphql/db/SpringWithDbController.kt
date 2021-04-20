package com.foo.graphql.db

import com.foo.graphql.SpringController
import com.p6spy.engine.spy.P6SpyDriver
import org.evomaster.client.java.controller.db.DbCleaner
import org.hibernate.dialect.H2Dialect
import org.springframework.boot.SpringApplication
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Connection
import java.sql.SQLException
import kotlin.random.Random.Default.nextInt


abstract class SpringWithDbController(applicationClass: Class<*>) : SpringController(applicationClass) {

    companion object{
        init {
            /**
             * To avoid issues with non-determinism checks (in particular in the handling of taint-analysis),
             * we must disable the cache in H2
             */
            System.setProperty("h2.objectCache", "false")
        }
    }

    var dbconnection : Connection? = null


    override fun startSut(): String {
        //lot of problem if using same H2 instance. see:
        //https://github.com/h2database/h2database/issues/227
        val rand = nextInt()

        ctx = SpringApplication.run(applicationClass,
            "--server.port=0",
            "--graphql.tools.schema-location-pattern=**/${schemaName()}",
            "--spring.datasource.url=jdbc:p6spy:h2:mem:testdb_"+rand+";DB_CLOSE_DELAY=-1;",
            "--spring.datasource.driver-class-name=" + P6SpyDriver::class.java.name,
            "--spring.jpa.database-platform=" + H2Dialect::class.java.name,
            "--spring.datasource.username=sa",
            "--spring.datasource.password",
            "--spring.jpa.properties.hibernate.show_sql=true"
        )

        if (dbconnection != null){
            try {
                dbconnection!!.close()
            }catch (e : SQLException){
                e.printStackTrace()
            }
        }
        ctx?:throw IllegalStateException("fail to initialize ctx")
        val jdbc = ctx!!.getBean(JdbcTemplate::class.java)

        try {
            dbconnection = jdbc.dataSource.connection
        }catch (e : SQLException){
            throw RuntimeException(e)
        }

        return "http://localhost:$sutPort"
    }


    override fun resetStateOfSUT() {
        if (dbconnection != null)
            DbCleaner.clearDatabase_H2(dbconnection)
    }

    override fun stopSut() {
        super.stopSut()
        dbconnection = null
    }

    override fun getConnection() = dbconnection

    override fun getDatabaseDriverName() = "org.h2.Driver"
}