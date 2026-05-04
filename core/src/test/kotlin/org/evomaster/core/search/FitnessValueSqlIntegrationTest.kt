package org.evomaster.core.search

import org.evomaster.client.java.sql.DbInfoExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.client.java.sql.internal.SqlHandler
import org.evomaster.client.java.controller.api.dto.database.execution.SqlExecutionLogDto
import org.evomaster.core.sql.DatabaseExecution
import org.evomaster.core.sql.schema.TableId
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.sql.Connection
import java.sql.DriverManager

class FitnessValueSqlIntegrationTest {

    companion object {

        private lateinit var connection: Connection

        @BeforeAll
        @JvmStatic
        fun initClass() {
            connection = DriverManager.getConnection("jdbc:h2:mem:db_test", "sa", "")
        }

        @AfterAll
        @JvmStatic
        fun clean() {
            connection.close()
        }
    }

    @BeforeEach
    fun initTest() {
        SqlScriptRunner.execCommand(connection, "DROP ALL OBJECTS;")
        SqlScriptRunner.execCommand(connection, """
            CREATE TABLE Person (
                person_id INT PRIMARY KEY,
                age INT
            );
        """)
    }

    private fun runSelect(sqlCommand: String): DatabaseExecution {
        val schema = DbInfoExtractor.extract(connection)
        val tableIds = schema.tables.map { t -> TableId(t.id.name) }.toSet()

        val handler = SqlHandler(null)
        handler.setConnection(connection)
        handler.setSchema(schema)

        handler.handle(SqlExecutionLogDto(sqlCommand, false, 0L))
        handler.getSqlDistances(null, true)

        return DatabaseExecution.fromDto(handler.getExecutionDto(), tableIds)
    }

    @Test
    fun testSelectWithFailedWhereIsCollected() {
        // Empty table: WHERE clause will fail (no rows match)
        val sqlCommand = "SELECT * FROM Person WHERE age = 30"

        val fv = FitnessValue(1.0)
        fv.setDatabaseExecution(0, runSelect(sqlCommand))
        fv.aggregateDatabaseData()

        val queries = fv.getViewOfAggregatedFailedWhereQueries()
        assertEquals(1, queries.size)
        assertTrue(queries.contains(sqlCommand))
    }

    @Test
    fun testSelectWithSuccessfulWhereIsNotCollected() {
        // Insert a row so the WHERE clause succeeds
        SqlScriptRunner.execCommand(connection, "INSERT INTO Person VALUES (1, 30)")
        val sqlCommand = "SELECT * FROM Person WHERE age = 30"

        val fv = FitnessValue(1.0)
        fv.setDatabaseExecution(0, runSelect(sqlCommand))
        fv.aggregateDatabaseData()

        val queries = fv.getViewOfAggregatedFailedWhereQueries()
        assertTrue(queries.isEmpty())
    }
}
