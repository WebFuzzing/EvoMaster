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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.sql.Connection
import java.sql.DriverManager

/**
 * A DATE column is encoded, like a TIMESTAMP one, as epoch seconds, and must be inserted as a date.
 *
 * It used to be inserted as the bare integer Z3 assigned to it, which the database rejects, so no row
 * of a table with a DATE column was ever inserted, whatever the query. The failure was silent:
 * SqlScriptRunner.execInsert logs a failed insertion and carries on.
 */
class DateColumnSolvingTest {

    companion object {
        private lateinit var solver: SMTLibZ3DbConstraintSolver
        private lateinit var connection: Connection
        private lateinit var schemaDto: DbInfoDto

        @JvmStatic
        @BeforeAll
        fun setup() {
            connection = DriverManager.getConnection("jdbc:h2:mem:date_column_test", "sa", "")
            SqlScriptRunner.execCommand(connection, "CREATE TABLE ev(id bigint primary key, d date);\n")
            schemaDto = DbInfoExtractor.extract(connection)
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
        SqlScriptRunner.execCommand(connection, "DELETE FROM ev;\n")
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "SELECT * FROM ev WHERE id = 1",
        "SELECT * FROM ev WHERE d = DATE '2024-03-05'",
        "SELECT * FROM ev WHERE d > DATE '2024-01-01'",
        "SELECT * FROM ev WHERE d > TIMESTAMP '2024-01-01 10:00:00'"
    ])
    fun generatedRowsAreInsertedAndSatisfyTheQuery(query: String) {
        val actions = solver.solve(schemaDto, query, 1)
        assertFalse(actions.isEmpty(), "Solver should return actions for a satisfiable query")

        val results = SqlScriptRunner.execInsert(connection, SqlActionTransformer.transform(actions).insertions)
        assertEquals(listOf(true), results.executionResults, "The generated row should be inserted")

        val result = SqlScriptRunner.execCommand(connection, query)
        assertFalse(result.isEmpty(), "Inserted rows should satisfy the original query")
    }
}
