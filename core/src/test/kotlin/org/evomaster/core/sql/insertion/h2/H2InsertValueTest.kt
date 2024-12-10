package org.evomaster.core.sql.insertion.h2

import org.evomaster.client.java.sql.SqlScriptRunner
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class H2InsertValueTest {


    companion object {

        private lateinit var connection: Connection

        @BeforeAll
        @JvmStatic
        fun initClass() {
            connection = DriverManager.getConnection("jdbc:h2:mem:db_test", "sa", "")
        }
    }

    @BeforeEach
    fun initTest() {
        //custom H2 command
        SqlScriptRunner.execCommand(connection, "DROP ALL OBJECTS;")
    }


    @Test
    fun testPoint() {
        SqlScriptRunner.execCommand(connection, "CREATE TABLE SpatialTable(pointcolumn GEOMETRY(POINT) NOT NULL)")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(pointcolumn) VALUES ('POINT(-1.0 -1.0)')")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(pointcolumn) VALUES ('POINT(0.0 0.0)')")
    }

    @Test
    fun testMultipoint() {
        SqlScriptRunner.execCommand(connection, "CREATE TABLE SpatialTable(multipointcolumn GEOMETRY(MULTIPOINT) NOT NULL)")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(multipointcolumn) VALUES ('MULTIPOINT EMPTY')")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(multipointcolumn) VALUES ('MULTIPOINT((0 0))')")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(multipointcolumn) VALUES ('MULTIPOINT((0 0), (1 1))')")
    }

    @Test
    fun testLinestring() {
        SqlScriptRunner.execCommand(connection, "CREATE TABLE SpatialTable(linestringcolumn GEOMETRY(LINESTRING) NOT NULL)")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(linestringcolumn) VALUES ('LINESTRING EMPTY')")

        assertThrows(SQLException::class.java) {
            // expect linestring to fail if only one point
            SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(linestringcolumn) VALUES ('LINESTRING (0.0 0.0)')")
        }
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(linestringcolumn) VALUES ('LINESTRING (0.0 0.0, 0.0 0.0)')")
    }

    @Test
    fun testMultilinestring() {
        SqlScriptRunner.execCommand(connection, "CREATE TABLE SpatialTable(multilinestringcolumn GEOMETRY(MULTILINESTRING) NOT NULL)")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(multilinestringcolumn) VALUES ('MULTILINESTRING EMPTY')")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(multilinestringcolumn) VALUES ('MULTILINESTRING((0.0 0.0, 1.0 1.0))')")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(multilinestringcolumn) VALUES ('MULTILINESTRING((0.0 0.0, 1.0 1.0), (0.0 0.0, 1.0 1.0))')")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(multilinestringcolumn) VALUES ('MULTILINESTRING((0.0 0.0, 1.0 1.0), EMPTY)')")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(multilinestringcolumn) VALUES ('MULTILINESTRING(EMPTY)')")
    }

    @Test
    fun testGeometryCollection() {
        SqlScriptRunner.execCommand(connection, "CREATE TABLE SpatialTable(geometrycollectioncolumn GEOMETRY(GEOMETRYCOLLECTION) NOT NULL)")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(geometrycollectioncolumn) VALUES ('GEOMETRYCOLLECTION(POINT(0 0))')")
    }

}