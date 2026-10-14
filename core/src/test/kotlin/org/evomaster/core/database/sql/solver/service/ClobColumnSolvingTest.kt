package org.evomaster.core.database.sql.solver.service

import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto
import org.evomaster.client.java.sql.DbInfoExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.database.sql.SqlActionTransformer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager

/**
 * H2 1.4 reports a CLOB column with the type name CLOB, which was missing from the type map. Since
 * every table of the schema is declared for every query, a single CLOB column made the generation
 * fail for all the queries of the schema, including those that never read it.
 */
class ClobColumnSolvingTest {

    companion object {
        private lateinit var solver: SMTLibZ3DbConstraintSolver
        private lateinit var connection: Connection
        private lateinit var schemaDto: DbInfoDto

        @JvmStatic
        @BeforeAll
        fun setup() {
            connection = DriverManager.getConnection("jdbc:h2:mem:clob_column_test", "sa", "")
            SqlScriptRunner.execCommand(connection, "CREATE TABLE project(id bigint primary key, name clob);\n")
            schemaDto = DbInfoExtractor.extract(connection)
            // H2 2.x reports the column as CHARACTER LARGE OBJECT, already mapped; H2 1.4 reports CLOB
            schemaDto.tables.single().columns.single { it.name.equals("NAME", ignoreCase = true) }.type = "CLOB"
            solver = SMTLibZ3DbConstraintSolver()
            solver.initializeExecutor()
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            connection.close()
            if (this::solver.isInitialized) {
                solver.close()
            }
        }
    }

    @BeforeEach
    fun clean() {
        SqlScriptRunner.execCommand(connection, "DELETE FROM project;\n")
    }

    @Test
    fun `a query on a table with a CLOB column is solved`() {
        val query = "SELECT * FROM project WHERE name = 'evomaster'"
        val actions = solver.solve(schemaDto, query, 1)
        assertEquals(1, actions.size)

        val results = SqlScriptRunner.execInsert(connection, SqlActionTransformer.transform(actions).insertions)
        assertEquals(listOf(true), results.executionResults)
        assertFalse(SqlScriptRunner.execCommand(connection, query).isEmpty())
    }
}
