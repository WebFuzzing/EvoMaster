package org.evomaster.core.sql.insertion.postgres

import com.google.gson.Gson
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.PostgresContainerUtils
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.postgresql.util.PGobject
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class PostgresInsertValueTest {

    companion object {

        @JvmStatic
        protected lateinit var connection: Connection

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
        fun clean() {
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
    }

    @Test
    fun testPoint() {
        SqlScriptRunner.execCommand(connection, "CREATE TABLE SpatialTable(pointColumn POINT NOT NULL)")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(pointColumn) VALUES ('(0.0, 0.0)')")
    }


    @Test
    fun testPolygon() {
        SqlScriptRunner.execCommand(connection, "CREATE TABLE SpatialTable(polygonColumn POLYGON NOT NULL)")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(polygonColumn) VALUES ('((0.0, 1.0), (1.0, 1.0), (0.0, 0.0))')")
    }

    @Test
    fun testPath() {
        SqlScriptRunner.execCommand(connection, "CREATE TABLE SpatialTable(pathColumn PATH NOT NULL)")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(pathColumn) VALUES ('((0.0, 0.0), (0.0, 1.0))')")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(pathColumn) VALUES ('((0.0, 0.0), (0.0, 0.0))')")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(pathColumn) VALUES ('((0.0, 0.0))')")
        assertThrows<SQLException> {
            // empty path is invalid
            SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(pathColumn) VALUES ('()')")
        }
    }


    @Test
    fun testCircle() {
        SqlScriptRunner.execCommand(connection, "CREATE TABLE SpatialTable(circleColumn CIRCLE NOT NULL)")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(circleColumn) VALUES ('((0.0, 0.0), 1.0)')")
    }

    @Test
    fun testBox() {
        SqlScriptRunner.execCommand(connection, "CREATE TABLE SpatialTable(boxColumn BOX NOT NULL)")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(boxColumn) VALUES ('((0.0, 1.0), (2.0, 3.0))')")
    }

    @Test
    fun testLine() {
        SqlScriptRunner.execCommand(connection, "CREATE TABLE SpatialTable(lineColumn LINE NOT NULL)")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(lineColumn) VALUES ('((0.0, 0.0), (0.0, 1.0))')")
    }

    @Test
    fun testLineSegment() {
        SqlScriptRunner.execCommand(connection, "CREATE TABLE SpatialTable(lineSegmentColumn LSEG NOT NULL)")
        SqlScriptRunner.execCommand(connection, "INSERT INTO SpatialTable(lineSegmentColumn) VALUES ('((0.0, 0.0), (0.0, 1.0))')")
    }

    @Test
    fun testInsertJson() {
        val fooDto = FooDto(-1)
        val json = Gson().toJson(fooDto)

        SqlScriptRunner.execCommand(connection, "CREATE TABLE JSONTable(jsonColumn json NOT NULL)")
        SqlScriptRunner.execCommand(connection, "INSERT INTO JSONTable(jsonColumn) VALUES ('${json}')")

        val queryResult = SqlScriptRunner.execCommand(connection, "SELECT jsonColumn FROM JSONTable")
        assertFalse(queryResult.isEmpty)

        val row = queryResult.seeRows()[0]
        val jsonValue = row.getValueByName("jsonColumn") as PGobject
        val jsonFromDB = jsonValue.toString()

        val dto = Gson().fromJson(jsonFromDB, FooDto::class.java)

        assertEquals(-1, dto.x)
    }

}

class FooDto(var x : Int)
