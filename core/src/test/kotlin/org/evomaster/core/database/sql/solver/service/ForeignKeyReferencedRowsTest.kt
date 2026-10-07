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
 * The formula always asserts every foreign key of the schema, so a row of a referencing table is
 * forced to point at a row of the referenced table. When the query mentions only the referencing
 * table, the referenced row must still be requested from Z3 and inserted: otherwise the generated
 * INSERT points at a row that does not exist and the database rejects it.
 *
 * The referenced rows must also be inserted first. Z3's answer carries no order, so the rows are
 * sorted by the foreign keys between their tables.
 */
class ForeignKeyReferencedRowsTest {

    companion object {
        private lateinit var solver: SMTLibZ3DbConstraintSolver
        private lateinit var connection: Connection
        private lateinit var schemaDto: DbInfoDto

        @JvmStatic
        @BeforeAll
        fun setup() {
            connection = DriverManager.getConnection("jdbc:h2:mem:fk_referenced_rows_test", "sa", "")
            SqlScriptRunner.execCommand(connection,
                "CREATE TABLE author(id bigint primary key, name varchar(255));\n" +
                "CREATE TABLE book(id bigint primary key, pages int, author bigint not null, " +
                "FOREIGN KEY (author) REFERENCES author(id));\n" +
                "CREATE TABLE chapter(id bigint primary key, number int, book bigint not null, " +
                "FOREIGN KEY (book) REFERENCES book(id));\n")
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
        SqlScriptRunner.execCommand(connection, "DELETE FROM chapter; DELETE FROM book; DELETE FROM author;\n")
    }

    @Test
    fun rowsReferencedByForeignKeysAreInsertedToo() {
        insertSolutionAndQuery("SELECT * FROM book WHERE pages > 10", 1)
    }

    /**
     * A chain of two foreign keys, with two rows per table: the referenced rows are requested
     * transitively, and every row is inserted after the one it references. With these table names
     * the parser's hash map happens to yield book rows before author rows, so this also fails if the
     * rows are not sorted.
     */
    @Test
    fun rowsReferencedTransitivelyAreInsertedInDependencyOrder() {
        insertSolutionAndQuery("SELECT * FROM chapter WHERE number > 3", 2)
    }

    private fun insertSolutionAndQuery(selectQuery: String, numberOfRows: Int) {
        val actions = solver.solve(schemaDto, selectQuery, numberOfRows)
        assertFalse(actions.isEmpty(), "Solver should return actions for a satisfiable query")

        val dto = SqlActionTransformer.transform(actions)
        SqlScriptRunner.execInsert(connection, dto.insertions)

        val result = SqlScriptRunner.execCommand(connection, selectQuery)
        assertFalse(result.isEmpty(), "Inserted rows should satisfy the original SELECT query")
    }
}
