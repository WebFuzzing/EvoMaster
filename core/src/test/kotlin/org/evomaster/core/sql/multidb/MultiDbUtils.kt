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


    private const val POSTGRES_VERSION: String = "14";
    private const val POSTGRES_PORT: Int = 5432
    private val postgres = KGenericContainer("postgres:$POSTGRES_VERSION")
        .withExposedPorts(POSTGRES_PORT)
        //https://www.postgresql.org/docs/current/auth-trust.html
        .withEnv("POSTGRES_HOST_AUTH_METHOD","trust")
        .withTmpFs(Collections.singletonMap("/var/lib/postgresql/data", "rw"))


    private const val MYSQL_DB_NAME = "test"
    private const val MYSQL_PORT = 3306
    private const val MYSQL_VERSION = "8.0.27";
    private val mysql = KGenericContainer("mysql:$MYSQL_VERSION")
        .withEnv(
            mutableMapOf(
                "MYSQL_ROOT_PASSWORD" to "root",
                "MYSQL_DATABASE" to MYSQL_DB_NAME,  // TODO can this be removed?
                "MYSQL_USER" to "test",
                "MYSQL_PASSWORD" to "test"
            )
        )
        .withExposedPorts(MYSQL_PORT)


    fun createConnection(name: String, type: DatabaseType) : Connection {

        return when (type) {
            DatabaseType.MYSQL -> {
                val host = mysql.host
                val port = mysql.getMappedPort(MYSQL_PORT)
                val url = "jdbc:mysql://$host:$port/$name"
                DriverManager.getConnection(url, "test", "test")
            }
            DatabaseType.H2 -> {
                DriverManager.getConnection("jdbc:h2:mem:$name", "sa", "")
            }
            DatabaseType.POSTGRES -> {
                val host = postgres.host
                val port = postgres.getMappedPort(POSTGRES_PORT)
                val url = "jdbc:postgresql://$host:$port/postgres"  //TODO check name
                /*
                 * A call to getConnection()  when the postgres container is still not ready,
                 * signals a PSQLException with message "FATAL: the database system is starting up".
                 * The following issue describes how to avoid this by using a LogMessageWaitStrategy
                 * https://github.com/testcontainers/testcontainers-java/issues/317
                 */
                postgres.waitingFor(LogMessageWaitStrategy().withRegEx(".*database system is ready to accept connections.*\\s").withTimes(2))

                DriverManager.getConnection(url, "postgres", "")
            }
            else -> throw IllegalArgumentException("Unsupported database type: ${type.name}")
        }
    }

    fun resetDatabase(type: DatabaseType, connection: Connection) {
        when(type) {
            DatabaseType.H2 -> SqlScriptRunner.execCommand(connection, "DROP ALL OBJECTS;")
            //FIXME schema names need fixing. also is this actually dropping tables???
            DatabaseType.MYSQL -> DbCleaner.dropDatabaseTables(connection, MYSQL_DB_NAME, null, DatabaseType.MYSQL)
            DatabaseType.POSTGRES -> {
                //TODO what about other schemas/catalogs?
                SqlScriptRunner.execCommand(connection, "DROP SCHEMA public CASCADE;")
                SqlScriptRunner.execCommand(connection, "CREATE SCHEMA public;")
                SqlScriptRunner.execCommand(connection, "GRANT ALL ON SCHEMA public TO postgres;")
                SqlScriptRunner.execCommand(connection, "GRANT ALL ON SCHEMA public TO public;")
            }
            else -> throw IllegalArgumentException("Unsupported database type: ${type.name}")
        }
    }

    fun startDatabase(type: DatabaseType) {
        when(type) {
            DatabaseType.H2 -> { /* nothing to do? started automatically on connection*/}
            DatabaseType.MYSQL -> mysql.start()
            DatabaseType.POSTGRES -> postgres.start()
            else -> throw IllegalArgumentException("Unsupported database type: ${type.name}")
        }
    }

    fun stopDatabase(type: DatabaseType) {
        when(type) {
            DatabaseType.H2 -> {/* nothing to do*/ }
            DatabaseType.MYSQL -> mysql.stop()
            DatabaseType.POSTGRES -> postgres.stop()
            else -> throw IllegalArgumentException("Unsupported database type: ${type.name}")
        }
    }
}