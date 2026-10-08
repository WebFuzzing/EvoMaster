package org.evomaster.core.database.sql.solver

import net.sf.jsqlparser.parser.CCJSqlParserUtil
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto
import org.evomaster.client.java.sql.DbInfoExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager

/**
 * A column whose type has no SMT-LIB mapping only affects the queries that read its table.
 *
 * Every table of the schema is declared for every query, so one such column used to make generation
 * fail for all queries against the schema, even those on unrelated tables.
 */
class UnmappedColumnTypeTest {

    companion object {
        private lateinit var connection: Connection
        private lateinit var schemaDto: DbInfoDto

        @JvmStatic
        @BeforeAll
        fun setup() {
            connection = DriverManager.getConnection("jdbc:h2:mem:unmapped_column_type_test", "sa", "")
            SqlScriptRunner.execCommand(
                connection,
                "CREATE TABLE users(id bigint primary key, name varchar(255));\n" +
                    "CREATE TABLE blobs(id bigint primary key, payload blob);\n" +
                    "CREATE TABLE blob_refs(id bigint primary key, blob_id bigint REFERENCES blobs(id));\n"
            )
            schemaDto = DbInfoExtractor.extract(connection)
        }

        @JvmStatic
        @AfterAll
        fun tearDown() {
            connection.close()
        }
    }

    private fun generate(query: String): String =
        SmtLibGenerator(schemaDto, 1).generateSMT(CCJSqlParserUtil.parse(query)).toString()

    @Test
    fun `a query on another table is still translated`() {
        val smt = generate("SELECT * FROM users WHERE name = 'x'")

        assertTrue(smt.contains("(get-value (users__1))")) { "expected the users row to be requested:\n$smt" }
        assertFalse(smt.contains("blobs", ignoreCase = true)) { "the unmapped table must not be declared:\n$smt" }
    }

    @Test
    fun `a query on the table with the unmapped column is rejected`() {
        val e = assertThrows(RuntimeException::class.java) { generate("SELECT * FROM blobs WHERE id = 1") }

        assertTrue(e.message!!.contains("Unsupported column type")) { e.message }
    }

    @Test
    fun `a query on a table referencing it through a foreign key is rejected`() {
        // Its rows would reference rows of a table that is not declared, and could never be inserted
        assertThrows(RuntimeException::class.java) { generate("SELECT * FROM blob_refs WHERE id = 1") }
    }
}
