package org.evomaster.core.database.sql.solver.service

import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto
import org.evomaster.client.java.sql.DbInfoExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager

/**
 * A table whose name contains non-ASCII characters is declared in SMT-LIB under its ASCII-folded
 * name, so the row constants Z3 returns carry that folded name ("categoria__1" for "Categoría").
 * Mapping a solution back to the schema must fold the schema names the same way; comparing against
 * the original names finds no table and the whole solution is lost.
 */
class NonAsciiTableNameTest {

    companion object {
        private lateinit var solver: SMTLibZ3DbConstraintSolver
        private lateinit var connection: Connection
        private lateinit var schemaDto: DbInfoDto

        @JvmStatic
        @BeforeAll
        fun setup() {
            connection = DriverManager.getConnection("jdbc:h2:mem:non_ascii_table_test", "sa", "")
            SqlScriptRunner.execCommand(connection, "CREATE TABLE categoría(id bigint primary key, nombre varchar(255));\n")
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

    @Test
    fun solutionForNonAsciiTableIsMappedBackToTheOriginalTable() {

        val newActions = solver.solve(schemaDto, "SELECT * FROM categoría WHERE nombre = 'Libros';", 1)

        assertEquals(1, newActions.size)
        assertEquals("CATEGORÍA", newActions[0].table.id.name.uppercase())

        val nombre = newActions[0].seeTopGenes().first { it.name.equals("NOMBRE", ignoreCase = true) }
        assertEquals("Libros", (nombre as StringGene).value)
    }
}
