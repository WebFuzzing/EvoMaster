package org.evomaster.core.sql.multidb

import org.evomaster.client.java.controller.api.dto.database.schema.DatabaseType
import org.evomaster.client.java.sql.DbCleaner
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.KGenericContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

object MultiDbUtils {


    private const val POSTGRES_VERSION: String = "17"
    private const val POSTGRES_PORT: Int = 5432
    private val postgres = KGenericContainer("postgres:$POSTGRES_VERSION")
        .withExposedPorts(POSTGRES_PORT)
        //https://www.postgresql.org/docs/current/auth-trust.html
        .withEnv("POSTGRES_HOST_AUTH_METHOD","trust")
        .withTmpFs(Collections.singletonMap("/var/lib/postgresql/data", "rw"))


  //  private const val MYSQL_DB_NAME = "test"
    private const val MYSQL_PORT = 3306
    private const val MYSQL_VERSION = "8.0.27"
    private val mysql = KGenericContainer("mysql:$MYSQL_VERSION")
        .withEnv(
            mutableMapOf(
                "MYSQL_ROOT_PASSWORD" to "root",
                "MYSQL_USER" to "test",
                "MYSQL_PASSWORD" to "test"
            )
        )
        .withTmpFs(Collections.singletonMap("/var/lib/mysql", "rw"))
        .withExposedPorts(MYSQL_PORT)


    fun getMySQLBaseUrl() : String{
        val host = mysql.host
        val port = mysql.getMappedPort(MYSQL_PORT)
        return "jdbc:mysql://$host:$port/"
    }

    fun getPostgresBaseUrl() : String{
        val host = postgres.host
        val port = postgres.getMappedPort(POSTGRES_PORT)
        return "jdbc:postgresql://$host:$port/"
    }

    fun getH2BaseUrl() : String{
        return "jdbc:h2:mem:"
    }

    fun getBaseUrl(type: DatabaseType) : String{
        return when(type){
            DatabaseType.POSTGRES -> getPostgresBaseUrl()
            DatabaseType.MYSQL -> getMySQLBaseUrl()
            DatabaseType.H2 -> getH2BaseUrl()
            else -> throw IllegalArgumentException("Unsupported database type: $type")
        }
    }

    fun createConnection(name: String, type: DatabaseType) : Connection {

        return when (type) {
            DatabaseType.MYSQL -> {
                DriverManager.getConnection("${getMySQLBaseUrl()}$name?allowMultiQueries=true", "root", "root")
            }
            DatabaseType.H2 -> {
                DriverManager.getConnection("${getH2BaseUrl()}$name", "sa", "")
            }
            DatabaseType.POSTGRES -> {
                DriverManager.getConnection("${getPostgresBaseUrl()}$name", "postgres", "")
            }
            else -> throw IllegalArgumentException("Unsupported database type: ${type.name}")
        }
    }

    fun resetDatabase(name: String, type: DatabaseType) {
        when(type) {
            DatabaseType.H2 -> {
                val connection = createConnection(name, type)
                connection.use {
                    SqlScriptRunner.execCommand(connection, "DROP ALL OBJECTS;")
                }
            }
            DatabaseType.MYSQL ->{
                val main = DriverManager.getConnection(getMySQLBaseUrl(), "root", "root")
                main.use {
                    SqlScriptRunner.execCommand(main, "DROP DATABASE IF EXISTS $name")
                    SqlScriptRunner.execCommand(main, "CREATE DATABASE $name")
                }
            }
            DatabaseType.POSTGRES -> {
                val main = DriverManager.getConnection(getPostgresBaseUrl(), "postgres", "")
                main.use {
                    SqlScriptRunner.execCommand(main, "DROP DATABASE IF EXISTS $name")
                    SqlScriptRunner.execCommand(main, "CREATE DATABASE $name")
                }
            }
            else -> throw IllegalArgumentException("Unsupported database type: ${type.name}")
        }
    }

    fun startDatabase(type: DatabaseType) {
        when(type) {
            DatabaseType.H2 -> { /* nothing to do? started automatically on connection*/}
            DatabaseType.MYSQL -> mysql.start()
            DatabaseType.POSTGRES -> {
                postgres.start()
                /*
               * A call to getConnection()  when the postgres container is still not ready,
               * signals a PSQLException with message "FATAL: the database system is starting up".
               * The following issue describes how to avoid this by using a LogMessageWaitStrategy
               * https://github.com/testcontainers/testcontainers-java/issues/317
               */
                postgres.waitingFor(LogMessageWaitStrategy().withRegEx(".*database system is ready to accept connections.*\\s").withTimes(2))
            }
            else -> throw IllegalArgumentException("Unsupported database type: ${type.name}")
        }
    }

    fun stopDatabase(type: DatabaseType) {
        when(type) {
            DatabaseType.H2 -> {/* nothing to do*/ }
            DatabaseType.MYSQL -> mysql.stop()
            DatabaseType.POSTGRES -> {
                postgres.stop()
            }
            else -> throw IllegalArgumentException("Unsupported database type: ${type.name}")
        }
    }

    fun stopAllDatabases() {
        mysql.stop()
        postgres.stop()
    }
}