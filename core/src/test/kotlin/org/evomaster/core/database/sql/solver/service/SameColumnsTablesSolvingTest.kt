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
 * The SMT constructor of a table is named after its columns, so two tables with the same columns
 * share it. Z3 then writes their values qualified with the sort, ((as id-body NoteRow) 1 "x"), which
 * the parser did not read: the solution came back empty, and no data was generated for any query on
 * either table. Lookup tables of the form (id, name) are a common case.
 */
class SameColumnsTablesSolvingTest {

    companion object {
        private lateinit var solver: SMTLibZ3DbConstraintSolver
        private lateinit var connection: Connection
        private lateinit var schemaDto: DbInfoDto

        @JvmStatic
        @BeforeAll
        fun setup() {
            connection = DriverManager.getConnection("jdbc:h2:mem:same_columns_tables_test", "sa", "")
            SqlScriptRunner.execCommand(
                connection,
                "CREATE TABLE doc(id bigint primary key, body varchar(20));\n" +
                    "CREATE TABLE note(id bigint primary key, body varchar(20));\n"
            )
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
        SqlScriptRunner.execCommand(connection, "DELETE FROM doc; DELETE FROM note;\n")
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "SELECT * FROM doc WHERE body = 'x'",
        "SELECT * FROM note WHERE body = 'y'"
    ])
    fun generatedRowsAreInsertedAndSatisfyTheQuery(query: String) {
        val actions = solver.solve(schemaDto, query, 1)
        assertFalse(actions.isEmpty(), "Solver should return actions for a satisfiable query")

        val results = SqlScriptRunner.execInsert(connection, SqlActionTransformer.transform(actions).insertions)
        assertEquals(actions.map { true }, results.executionResults, "Every generated row should be inserted")

        assertFalse(SqlScriptRunner.execCommand(connection, query).isEmpty(), "Inserted rows should satisfy the query")
    }
}
