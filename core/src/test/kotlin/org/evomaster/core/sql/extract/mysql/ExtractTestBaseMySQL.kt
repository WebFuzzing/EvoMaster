package org.evomaster.core.sql.extract.mysql

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.DbCleaner
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.KGenericContainer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import java.sql.Connection
import java.sql.DriverManager

abstract class ExtractTestBaseMySQL {

    companion object {

        @JvmStatic
        protected lateinit var connection: Connection

        private var sqlSchemaCommand : String? = null

        private const val MYSQL_DB_NAME = "test"

        private const val MYSQL_PORT = 3306

        private const val MYSQL_VERSION = "8.0.27";

        private val mysql = KGenericContainer("mysql:$MYSQL_VERSION")
        .withEnv(
            mutableMapOf(
                "MYSQL_ROOT_PASSWORD" to "root",
                "MYSQL_DATABASE" to MYSQL_DB_NAME,
                "MYSQL_USER" to "test",
                "MYSQL_PASSWORD" to "test"

            )
        )
        .withExposedPorts(MYSQL_PORT)

        @BeforeAll
        @JvmStatic
        fun initClass() {
            mysql.start()
            val host = mysql.containerIpAddress
            val port = mysql.getMappedPort(MYSQL_PORT)
            val url = "jdbc:mysql://$host:$port/$MYSQL_DB_NAME"

            connection = DriverManager.getConnection(url, "test", "test")
        }

        @AfterAll
        @JvmStatic
        fun clean(){
            sqlSchemaCommand = null
            connection.close()
            mysql.stop()
        }
    }

    @BeforeEach
    fun initTest() {

        if(sqlSchemaCommand == null){
            sqlSchemaCommand = this::class.java.getResource(getSchemaLocation()).readText()
        }

        SqlScriptRunner.execScript(connection, sqlSchemaCommand)
    }

    @AfterEach
    fun afterTest(){
        DbCleaner.dropDatabaseTables(connection, MYSQL_DB_NAME, null, DatabaseType.MYSQL)
    }


    abstract fun getSchemaLocation() : String
}