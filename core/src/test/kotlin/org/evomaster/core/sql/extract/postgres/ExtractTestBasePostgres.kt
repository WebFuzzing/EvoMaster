package org.evomaster.core.sql.extract.postgres

import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.PostgresContainerUtils
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import java.sql.Connection
import java.sql.DriverManager

/**
 * Created by arcuri82 on 01-Apr-19.
 */
abstract class ExtractTestBasePostgres {

    companion object {

        @JvmStatic
        protected lateinit var connection: Connection

        private var sqlSchemaCommand : String? = null

        private val postgres = PostgresContainerUtils.newContainer()

        @BeforeAll
        @JvmStatic
        fun initClass() {
            postgres.start()
            val jdbcUrl = PostgresContainerUtils.getJdbcUrl(postgres)
            connection = DriverManager.getConnection(jdbcUrl, "postgres", "")
        }

        @AfterAll
        @JvmStatic
        fun clean(){
            sqlSchemaCommand = null
            connection.close()
            postgres.stop()
        }
    }


    @BeforeEach
    fun initTest() {

        /*
            see:
            https://stackoverflow.com/questions/3327312/how-can-i-drop-all-the-tables-in-a-postgresql-database
         */

        SqlScriptRunner.execCommand(connection, "DROP SCHEMA public CASCADE;")
        SqlScriptRunner.execCommand(connection, "CREATE SCHEMA public;")
        SqlScriptRunner.execCommand(connection, "GRANT ALL ON SCHEMA public TO postgres;")
        SqlScriptRunner.execCommand(connection, "GRANT ALL ON SCHEMA public TO public;")

        if(sqlSchemaCommand == null){
            sqlSchemaCommand = this::class.java.getResource(getSchemaLocation()).readText()
        }

        SqlScriptRunner.execCommand(connection, sqlSchemaCommand)
    }

    abstract fun getSchemaLocation() : String

}
