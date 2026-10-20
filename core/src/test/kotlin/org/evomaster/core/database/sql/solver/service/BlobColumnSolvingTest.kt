package org.evomaster.core.database.sql.solver.service

import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto
import org.evomaster.client.java.sql.DbInfoExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.database.sql.SqlActionTransformer
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager

/**
 * A BLOB column had no entry in the type map. Since every table of the schema is declared for every
 * query, a single BLOB column made the generation fail for all the queries of the schema.
 *
 * Its value must also be one the database accepts: H2 1.4 reads a string literal given for a binary
 * column as hexadecimal, and rejected the INSERT of a value such as "abc".
 */
class BlobColumnSolvingTest {

    companion object {
        private lateinit var solver: SMTLibZ3DbConstraintSolver
        private lateinit var connection: Connection
        private lateinit var schemaDto: DbInfoDto

        @JvmStatic
        @BeforeAll
        fun setup() {
            connection = DriverManager.getConnection("jdbc:h2:mem:blob_column_test", "sa", "")
            SqlScriptRunner.execCommand(
                connection,
                "CREATE TABLE file(id bigint primary key, name varchar(20), data blob);\n"
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

        private fun dataColumn() =
            schemaDto.tables.single().columns.single { it.name.equals("DATA", ignoreCase = true) }
    }

    @BeforeEach
    fun clean() {
        SqlScriptRunner.execCommand(connection, "DELETE FROM file;\n")
    }

    @Test
    fun `a query on a table with a BLOB column is solved`() {
        assertEquals("BINARY LARGE OBJECT", dataColumn().type.uppercase()) // as H2 2.x reports it
        val query = "SELECT * FROM file WHERE name = 'report'"

        val actions = solver.solve(schemaDto, query, 1)
        assertEquals(1, actions.size)

        val results = SqlScriptRunner.execInsert(connection, SqlActionTransformer.transform(actions).insertions)
        assertEquals(listOf(true), results.executionResults)
        assertEquals(1, SqlScriptRunner.execCommand(connection, query).seeRows().size)
    }

    @Test
    fun `the value of a BLOB column is written in hexadecimal`() {
        val reported = dataColumn().type
        dataColumn().type = "BLOB" // as H2 1.4 reports it
        try {
            val actions = solver.solve(schemaDto, "SELECT * FROM file WHERE data = 'abc'", 1)
            assertEquals(1, actions.size)

            val data = actions.single().seeTopGenes().first { it.name.equals("DATA", ignoreCase = true) }
            assertEquals("616263", (data as StringGene).value)

            val results = SqlScriptRunner.execInsert(connection, SqlActionTransformer.transform(actions).insertions)
            assertEquals(listOf(true), results.executionResults)
        } finally {
            dataColumn().type = reported
        }
    }
}
