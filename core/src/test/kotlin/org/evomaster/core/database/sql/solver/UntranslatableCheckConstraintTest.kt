package org.evomaster.core.database.sql.solver

import net.sf.jsqlparser.parser.CCJSqlParserUtil
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto
import org.evomaster.client.java.sql.DbInfoExtractor
import org.evomaster.client.java.sql.SqlScriptRunner
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.sql.Connection
import java.sql.DriverManager

/**
 * A CHECK constraint that cannot be translated is skipped, like one that cannot be parsed.
 *
 * Every table is declared for every query, so letting the failure through made generation fail for
 * all queries against the schema, including those on unrelated tables.
 */
class UntranslatableCheckConstraintTest {

    companion object {
        private lateinit var connection: Connection
        private lateinit var schemaDto: DbInfoDto

        @JvmStatic
        @BeforeAll
        fun setup() {
            connection = DriverManager.getConnection("jdbc:h2:mem:untranslatable_check_test", "sa", "")
            SqlScriptRunner.execCommand(
                connection,
                // CHECK (a > 0) is translatable in both tables, and must still be asserted
                "CREATE TABLE t(id bigint primary key, a int, s varchar(20),\n" +
                    // fails in the condition parser: function calls are not supported
                    "  CHECK (LENGTH(s) > 2), CHECK (a > 0));\n" +
                    "CREATE TABLE v(id bigint primary key, a int, b int,\n" +
                    // parses, but fails in the SMT-LIB translation: an IN list holding a column
                    "  CHECK (a IN (b, 1)), CHECK (a > 0));\n" +
                    "CREATE TABLE u(id bigint primary key);\n"
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
        val smt = generate("SELECT * FROM u WHERE id = 1")

        assertTrue(smt.contains("(get-value (u__1))")) { "expected the u row to be requested:\n$smt" }
    }

    @Test
    fun `a CHECK the parser rejects is skipped and the others of the table are kept`() {
        val smt = generate("SELECT * FROM t WHERE id = 1")

        assertTrue(Regex("""\(>\s*\(A t__1\)\s*0\)""").containsMatchIn(smt)) {
            "expected CHECK (a > 0) to be asserted:\n$smt"
        }
    }

    @Test
    fun `a CHECK the translation rejects is skipped and the others of the table are kept`() {
        val smt = generate("SELECT * FROM v WHERE id = 1")

        assertTrue(Regex("""\(>\s*\(A v__1\)\s*0\)""").containsMatchIn(smt)) {
            "expected CHECK (a > 0) to be asserted:\n$smt"
        }
    }
}
