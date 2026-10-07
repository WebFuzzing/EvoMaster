package org.evomaster.core.database.sql.solver.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto
import org.evomaster.client.java.sql.DbInfoExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.database.sql.SqlActionTransformer
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.sql.Connection
import java.sql.DriverManager

/**
 * UUID and JSONB columns are encoded as SMT Strings with no constraint on their form. Unless the query
 * pins the value, Z3 picks something like "", and the INSERT of the whole row used to be rejected.
 * The failure was silent: SqlScriptRunner.execInsert logs a failed insertion and carries on.
 */
class UuidJsonColumnSolvingTest {

    companion object {
        private lateinit var solver: SMTLibZ3DbConstraintSolver
        private lateinit var connection: Connection
        private lateinit var schemaDto: DbInfoDto

        /**
         * JSONB is PostgreSQL's spelling, which H2 does not have, so a varchar column is reported as
         * JSONB on a second extraction of the schema. It is only checked for the value generated.
         */
        private lateinit var jsonbSchemaDto: DbInfoDto

        @JvmStatic
        @BeforeAll
        fun setup() {
            connection = DriverManager.getConnection("jdbc:h2:mem:uuid_json_column_test", "sa", "")
            SqlScriptRunner.execCommand(
                connection,
                "CREATE TABLE ev(id bigint primary key, u uuid);\n" +
                    "CREATE TABLE parent(id uuid primary key);\n" +
                    "CREATE TABLE child(id bigint primary key, parent_id uuid REFERENCES parent(id));\n" +
                    "CREATE TABLE doc(id bigint primary key, body varchar(255));\n"
            )
            schemaDto = DbInfoExtractor.extract(connection)
            jsonbSchemaDto = DbInfoExtractor.extract(connection)
            jsonbSchemaDto.tables.single { it.id.name.equals("DOC", ignoreCase = true) }
                .columns.single { it.name.equals("BODY", ignoreCase = true) }
                .type = "jsonb"
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
        SqlScriptRunner.execCommand(connection, "DELETE FROM child; DELETE FROM parent; DELETE FROM ev;\n")
    }

    @ParameterizedTest
    @ValueSource(strings = [
        // the value is free: Z3 picks a string that is not a UUID
        "SELECT * FROM ev WHERE id = 1",
        // the value is pinned by the query, and must be kept
        "SELECT * FROM ev WHERE u = '123e4567-e89b-12d3-a456-426614174000'",
        // a foreign key between UUID columns: the mapped values must still be equal
        "SELECT * FROM child WHERE id = 1"
    ])
    fun generatedRowsAreInsertedAndSatisfyTheQuery(query: String) {
        val actions = solver.solve(schemaDto, query, 1)
        assertFalse(actions.isEmpty(), "Solver should return actions for a satisfiable query")

        val results = SqlScriptRunner.execInsert(connection, SqlActionTransformer.transform(actions).insertions)
        assertEquals(actions.map { true }, results.executionResults, "Every generated row should be inserted")

        val result = SqlScriptRunner.execCommand(connection, query)
        assertFalse(result.isEmpty(), "Inserted rows should satisfy the original query")
    }

    @Test
    fun jsonbValueIsAJsonDocument() {
        val actions = solver.solve(jsonbSchemaDto, "SELECT * FROM doc WHERE id = 1", 1)

        val body = actions.single().seeTopGenes().first { it.name.equals("BODY", ignoreCase = true) } as StringGene
        val mapper = ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        assertFalse(mapper.readTree(body.value).isMissingNode, "'${body.value}' is not a JSON document")
    }
}
