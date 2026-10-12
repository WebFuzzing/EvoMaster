package org.evomaster.core.database.sql.solver.service

import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto
import org.evomaster.client.java.sql.DbInfoExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.database.sql.SqlActionTransformer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager

/**
 * A query combining SELECTs with UNION is parsed into a set-operation node rather than a plain SELECT.
 * The generator used to cast it to a plain SELECT, so the whole query failed with a
 * ClassCastException and no data was generated for it.
 */
class SetOperationSolvingTest {

    companion object {
        private lateinit var solver: SMTLibZ3DbConstraintSolver
        private lateinit var connection: Connection
        private lateinit var schemaDto: DbInfoDto

        @JvmStatic
        @BeforeAll
        fun setup() {
            connection = DriverManager.getConnection("jdbc:h2:mem:set_operation_test", "sa", "")
            SqlScriptRunner.execCommand(
                connection,
                "CREATE TABLE users(id bigint primary key, age int);\n" +
                    "CREATE TABLE products(id bigint primary key, stock int);\n"
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

    /**
     * A UNION returns rows as soon as one branch does, so data satisfying the first branch is enough.
     */
    @Test
    fun unionGeneratesDataForItsFirstBranch() {
        val query = "SELECT id FROM users WHERE age > 30 UNION SELECT id FROM products WHERE stock < 0"

        val actions = solver.solve(schemaDto, query, 1)
        assertFalse(actions.isEmpty(), "Solver should return actions for a satisfiable UNION")

        SqlScriptRunner.execInsert(connection, SqlActionTransformer.transform(actions).insertions)
        val result = SqlScriptRunner.execCommand(connection, query)
        assertFalse(result.isEmpty(), "Inserted rows should satisfy the original UNION query")
    }

    /**
     * INTERSECT constrains every branch, which the translation cannot express. It must be reported as
     * untranslatable, not crash.
     */
    @Test
    fun intersectIsRejectedWithoutCrashing() {
        val newActions = solver.solve(
            schemaDto,
            "SELECT id FROM users WHERE age > 30 INTERSECT SELECT id FROM products WHERE stock < 0",
            1
        )

        assertTrue(newActions.isEmpty())
    }
}
