package org.evomaster.core.sql.insertion.mysql

import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.KGenericContainer
import org.junit.jupiter.api.*
import org.testcontainers.containers.GenericContainer
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class MySQLInsertValueTest {

    companion object {

        private const val DB_NAME = "test"

        private const val PORT = 3306

        private const val MYSQL_VERSION = "8.0.27"

        private lateinit var connection: Connection

        private lateinit var mysql: GenericContainer<*>

        @BeforeAll
        @JvmStatic
        fun initClass() {
            connection = DriverManager.getConnection("jdbc:h2:mem:db_test", "sa", "")

            mysql = KGenericContainer("mysql:" + MYSQL_VERSION)
                    .apply {
                        withEnv(object : HashMap<String?, String?>() {
                            init {
                                put("MYSQL_ROOT_PASSWORD", "root")
                                put("MYSQL_DATABASE", DB_NAME)
                                put("MYSQL_USER", "test")
                                put("MYSQL_PASSWORD", "test")
                            }
                        })
                    }
                    .apply { withExposedPorts(PORT) }
            mysql.start()

        }
    }

    @BeforeEach
    fun initTest() {
        val host = mysql.containerIpAddress
        val port = mysql.getMappedPort(PORT)
        val url = "jdbc:mysql://$host:$port/$DB_NAME"
        connection = DriverManager.getConnection(url, "test", "test")

        SqlScriptRunner.execCommand(connection, "DROP DATABASE $DB_NAME;")
        SqlScriptRunner.execCommand(connection, "CREATE DATABASE $DB_NAME;")

        connection = DriverManager.getConnection(url, "test", "test")
    }


    @Test
    fun testPoint() {
        SqlScriptRunner.execCommand(connection, "CREATE TABLE SpatialTable(pointcolumn POINT NOT NULL)")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(pointcolumn) VALUES (POINT(-1,-1))")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(pointcolumn) VALUES (POINT(0,0))")
    }


    @Test
    fun testMultipoint() {
        SqlScriptRunner.execCommand(connection, "CREATE TABLE SpatialTable(multipointcolumn MULTIPOINT NOT NULL)")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(multipointcolumn) VALUES (MULTIPOINT(POINT(0,0),POINT(1,1)))")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(multipointcolumn) VALUES (MULTIPOINT(POINT(0,0)))")
        assertThrows<SQLException> {
            SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(multipointcolumn) VALUES (MULTIPOINT EMPTY)")
        }
        assertThrows<SQLException> {
            SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(multipointcolumn) VALUES (MULTIPOINT ())")
        }
    }

    @Test
    fun testLinestring() {
        SqlScriptRunner.execCommand(connection, "CREATE TABLE SpatialTable(linestringcolumn LINESTRING NOT NULL)")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(linestringcolumn) VALUES (LINESTRING(POINT(0,0),POINT(0,0)))")
        assertThrows<SQLException> {
            // failure Linestrings have at least two points
            SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(linestringcolumn) VALUES (LINESTRING())")
        }
        assertThrows<SQLException> {
            // failure Linestrings have at least two points
            SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(linestringcolumn) VALUES (LINESTRING(POINT(0,0)))")
        }
    }

    @Test
    fun testMultilinestring() {
        SqlScriptRunner.execCommand(connection, "CREATE TABLE SpatialTable(multilinestringcolumn MULTILINESTRING NOT NULL)")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(multilinestringcolumn) VALUES (MULTILINESTRING(LINESTRING(POINT(0,0),POINT(1,1))))")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(multilinestringcolumn) VALUES (MULTILINESTRING(LINESTRING(POINT(0,0),POINT(1,1)), LINESTRING(POINT(0,0),POINT(1,1))))")
        assertThrows<SQLException> {
            SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(multilinestringcolumn) VALUES (MULTILINESTRING())")
        }
    }


    @Test
    fun testPolygon() {
        SqlScriptRunner.execCommand(connection, "CREATE TABLE SpatialTable(polygoncolumn POLYGON NOT NULL)")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(polygoncolumn) VALUES (POLYGON( LINESTRING(POINT(0,0),POINT(0,0),POINT(1,1),POINT(0,0)) ))")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(polygoncolumn) VALUES (POLYGON( LINESTRING(POINT(0,0),POINT(0,0),POINT(0,0),POINT(0,0)) ))")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(polygoncolumn) VALUES (POLYGON(LINESTRING(POINT(0.0, 1.0), POINT(1.0, 1.0), POINT(0.0, 0.0), POINT(0.0, 1.0))))")
    }

    @Test
    fun testMultiPolygon() {
        SqlScriptRunner.execCommand(connection, "CREATE TABLE SpatialTable(multipolygoncolumn MULTIPOLYGON NOT NULL)")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(multipolygoncolumn) VALUES (MULTIPOLYGON(POLYGON( LINESTRING(POINT(0,0),POINT(0,0),POINT(1,1),POINT(0,0)))))")

    }

    @Test
    fun testGeometryCollection() {
        SqlScriptRunner.execCommand(connection, "CREATE TABLE SpatialTable(geometryCollectionColumn GEOMETRYCOLLECTION NOT NULL)")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(geometryCollectionColumn) VALUES (GEOMETRYCOLLECTION(POINT(0,0)))")
    }


}