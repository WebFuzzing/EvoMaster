package org.evomaster.core.database.sql.solver.service

import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto
import org.evomaster.client.java.sql.DbInfoExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.search.gene.BooleanGene
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.sql.Connection
import java.sql.DriverManager

/**
 * A BOOLEAN column is encoded as an SMT string restricted to "true" or "false". A boolean literal in
 * the WHERE clause must use the same spelling: SMT strings are case-sensitive, so encoding it as
 * "True" makes `WHERE active = true` unsatisfiable and no data is generated for the query.
 */
class BooleanColumnSolvingTest {

    companion object {
        private lateinit var solver: SMTLibZ3DbConstraintSolver
        private lateinit var connection: Connection
        private lateinit var schemaDto: DbInfoDto

        /**
         * The same table as reported by PostgreSQL, which names the type "bool". H2 always reports
         * "BOOLEAN", so the spelling is set on a second extraction of the schema.
         */
        private lateinit var boolSpellingSchemaDto: DbInfoDto

        @JvmStatic
        @BeforeAll
        fun setup() {
            connection = DriverManager.getConnection("jdbc:h2:mem:boolean_column_test", "sa", "")
            SqlScriptRunner.execCommand(connection, "CREATE TABLE account(id bigint primary key, active boolean);\n")
            schemaDto = DbInfoExtractor.extract(connection)
            boolSpellingSchemaDto = DbInfoExtractor.extract(connection)
            boolSpellingSchemaDto.tables.single().columns
                .single { it.name.equals("ACTIVE", ignoreCase = true) }
                .type = "bool"
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

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun booleanLiteralInWhereClauseIsSatisfiable(value: Boolean) {

        val newActions = solver.solve(schemaDto, "SELECT * FROM account WHERE active = $value;", 1)

        assertEquals(1, newActions.size)
        val active = newActions[0].seeTopGenes().first { it.name.equals("ACTIVE", ignoreCase = true) }
        assertEquals(value, (active as BooleanGene).value)
    }

    /**
     * A column declared `bool` must be restricted to the same two values as a `BOOLEAN` one. Without
     * that restriction `active <> false` is satisfied by any other string, typically "", which is
     * read back as false and so violates the query.
     */
    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun boolSpellingIsRestrictedToBooleanValues(value: Boolean) {

        val newActions = solver.solve(boolSpellingSchemaDto, "SELECT * FROM account WHERE active <> ${!value};", 1)

        assertEquals(1, newActions.size)
        val active = newActions[0].seeTopGenes().first { it.name.equals("ACTIVE", ignoreCase = true) }
        assertEquals(value, (active as BooleanGene).value)
    }
}
