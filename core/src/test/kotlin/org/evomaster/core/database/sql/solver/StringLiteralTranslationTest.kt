package org.evomaster.core.database.sql.solver

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A SQL string literal must reach Z3 with the same value it has in the query.
 *
 * SMT-LIB escapes a double quote inside a string as "". Written unescaped, the quote ends the
 * literal early and Z3 rejects the whole formula, so the query gets no data. An apostrophe, escaped
 * as '' in SQL, needs no escaping in SMT-LIB and must not be dropped.
 */
class StringLiteralTranslationTest {

    private val mapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private fun schema(): DbInfoDto = mapper.readValue(
        javaClass.getResourceAsStream("/solver/sample-schema.json")!!.bufferedReader().readText(),
        DbInfoDto::class.java
    )

    private fun generate(query: String): String =
        SmtLibGenerator(schema(), 1).generateSMT(CCJSqlParserUtil.parse(query)).toString()

    @Test
    fun `a double quote in a literal is escaped`() {
        val smt = generate("SELECT ID FROM ACCOUNT WHERE NAME = 'say \"hi\"'")

        assertTrue(smt.contains("\"say \"\"hi\"\"\"")) { "expected the escaped literal in:\n$smt" }
    }

    @Test
    fun `an apostrophe in a literal is kept`() {
        val smt = generate("SELECT ID FROM ACCOUNT WHERE NAME = 'O''Brien'")

        assertTrue(smt.contains("\"O'Brien\"")) { "expected the apostrophe to be kept in:\n$smt" }
    }

    @Test
    fun `literals in an IN list are escaped the same way`() {
        val smt = generate("SELECT ID FROM ACCOUNT WHERE NAME IN ('O''Brien', 'say \"hi\"')")

        assertTrue(smt.contains("\"O'Brien\"")) { "expected the apostrophe to be kept in:\n$smt" }
        assertTrue(smt.contains("\"say \"\"hi\"\"\"")) { "expected the escaped literal in:\n$smt" }
    }

    @Test
    fun `a character outside printable ASCII is written as a unicode escape`() {
        val smt = generate("SELECT ID FROM ACCOUNT WHERE NAME = 'ORDINÆR \\ 😀'")

        assertTrue(smt.contains("\"ORDIN\\u{c6}R \\u{5c} \\u{1f600}\"")) { "expected the escaped literal in:\n$smt" }
    }
}
