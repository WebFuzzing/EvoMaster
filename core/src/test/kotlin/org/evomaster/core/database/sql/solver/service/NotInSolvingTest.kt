package org.evomaster.core.database.sql.solver.service

import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto
import org.evomaster.client.java.sql.DbInfoExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.database.sql.SqlActionTransformer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager

/**
 * NOT IN excludes the listed values. It used to be read as a plain IN, so the solver produced rows
 * holding exactly the excluded values: they never satisfied the query, and a CHECK constraint written
 * with NOT IN made the database reject the insertion.
 */
class NotInSolvingTest {

    companion object {
        private lateinit var solver: SMTLibZ3DbConstraintSolver
        private lateinit var connection: Connection
        private lateinit var schemaDto: DbInfoDto

        @JvmStatic
        @BeforeAll
        fun setup() {
            connection = DriverManager.getConnection("jdbc:h2:mem:not_in_test", "sa", "")
            SqlScriptRunner.execCommand(connection,
                "CREATE TABLE item(id bigint primary key, status varchar(10));\n" +
                "CREATE TABLE ticket(id bigint primary key, state varchar(10), CHECK (state NOT IN ('CLOSED', 'DELETED')));\n")
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
        SqlScriptRunner.execCommand(connection, "DELETE FROM item; DELETE FROM ticket;\n")
    }

    @Test
    fun rowsSatisfyANotInQuery() {
        insertSolutionAndQuery("SELECT * FROM item WHERE status NOT IN ('A', 'B')")
    }

    @Test
    fun rowsSatisfyANotInCheckConstraint() {
        insertSolutionAndQuery("SELECT * FROM ticket WHERE id > 0")
    }

    private fun insertSolutionAndQuery(selectQuery: String) {
        val actions = solver.solve(schemaDto, selectQuery, 1)
        assertFalse(actions.isEmpty(), "Solver should return actions for a satisfiable query")

        val dto = SqlActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dto.insertions)

        val result = SqlScriptRunner.execCommand(connection, selectQuery)
        assertFalse(result.isEmpty(), "Inserted rows should satisfy the original SELECT query")
    }
}
