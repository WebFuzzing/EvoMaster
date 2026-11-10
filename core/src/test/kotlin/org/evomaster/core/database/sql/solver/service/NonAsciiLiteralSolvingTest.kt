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
 * A string literal with characters outside ASCII must reach the database unchanged.
 *
 * Z3 reads a string literal byte by byte and writes non-ASCII characters back as unicode escapes.
 * Written as raw UTF-8 and read back without decoding, 'ORDINÆR' came back as "ORDIN\u{c3}\u{86}R":
 * a row generated to satisfy an enumeration CHECK broke that same CHECK, and the database rejected
 * its INSERT. Enumerations with such values are common, e.g. in a Norwegian or Spanish schema.
 */
class NonAsciiLiteralSolvingTest {

    companion object {
        private lateinit var solver: SMTLibZ3DbConstraintSolver
        private lateinit var connection: Connection
        private lateinit var schemaDto: DbInfoDto

        @JvmStatic
        @BeforeAll
        fun setup() {
            connection = DriverManager.getConnection("jdbc:h2:mem:non_ascii_literal_test", "sa", "")
            SqlScriptRunner.execCommand(
                connection,
                "CREATE TABLE sak(id bigint primary key, kategori varchar(20), " +
                    "CHECK (kategori IN ('ORDINÆR', 'EØS')));\n"
            )
            schemaDto = DbInfoExtractor.extract(connection)
            /*
                H2 2.x reports the constraint with unicode-escaped literals, IN(U&'ORDIN\00c6R', ...),
                which the CHECK parser does not read. PostgreSQL and H2 1.4 report the characters as
                they are, so the constraint is given in that form. H2 still enforces the real one on
                insertion.
             */
            schemaDto.tables.single().tableCheckExpressions.single().sqlCheckExpression =
                "(kategori IN ('ORDINÆR', 'EØS'))"
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
        SqlScriptRunner.execCommand(connection, "DELETE FROM sak;\n")
    }

    @ParameterizedTest
    @ValueSource(strings = [
        // the value only comes from the CHECK
        "SELECT * FROM sak WHERE id = 1",
        // the value comes from the query too
        "SELECT * FROM sak WHERE kategori = 'EØS'"
    ])
    fun generatedRowsSatisfyTheCheckAndTheQuery(query: String) {
        val actions = solver.solve(schemaDto, query, 1)
        assertFalse(actions.isEmpty(), "Solver should return actions for a satisfiable query")

        val results = SqlScriptRunner.execInsert(connection, SqlActionTransformer.transform(actions).insertions)
        assertEquals(listOf(true), results.executionResults, "The generated row should satisfy the CHECK")

        val result = SqlScriptRunner.execCommand(connection, query)
        assertFalse(result.isEmpty(), "Inserted rows should satisfy the original query")
    }
}
