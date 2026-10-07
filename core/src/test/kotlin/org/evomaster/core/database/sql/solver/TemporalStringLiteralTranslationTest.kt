package org.evomaster.core.database.sql.solver

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A plain string literal compared against a TIMESTAMP (or DATE) column is encoded as epoch seconds,
 * like a typed `TIMESTAMP '...'` literal, since the column itself is an SMT Int. Written as an SMT
 * string, it made Z3 reject the formula for comparing an Int with a String.
 */
class TemporalStringLiteralTranslationTest {

    private val mapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private fun schema(): DbInfoDto = mapper.readValue(
        javaClass.getResourceAsStream("/solver/sample-schema.json")!!.bufferedReader().readText(),
        DbInfoDto::class.java
    )

    @Test
    fun `a string literal against a timestamp column is encoded as epoch seconds`() {
        val smt = SmtLibGenerator(schema(), 1)
            .generateSMT(CCJSqlParserUtil.parse("SELECT ID FROM ACCOUNT WHERE CREATED_AT > '2024-01-01'")).toString()

        assertTrue(smt.contains("1704067200")) { "expected the epoch of 2024-01-01 in:\n$smt" }
        assertFalse(smt.contains("\"2024-01-01\"")) { "the literal was written as a string:\n$smt" }
    }

    @Test
    fun `a date before 1970 is written as a negation`() {
        val smt = SmtLibGenerator(schema(), 1)
            .generateSMT(CCJSqlParserUtil.parse("SELECT ID FROM ACCOUNT WHERE CREATED_AT <> '1960-01-01'")).toString()

        assertTrue(smt.contains("(- 315619200)")) { "expected a negated numeral in:\n$smt" }
    }

    @Test
    fun `a string that is not a date drops only its condition`() {
        val generator = SmtLibGenerator(schema(), 1)
        val smt = generator.generateSMT(
            CCJSqlParserUtil.parse("SELECT ID FROM ACCOUNT WHERE CREATED_AT = 'yesterday' AND LEVEL = 3")
        ).toString()

        assertEquals(1, generator.skippedQueryConstraints)
        assertFalse(smt.contains("\"yesterday\"")) { "the literal was written as a string:\n$smt" }
        assertTrue(smt.contains("(LEVEL account__1)")) { "the other conjunct should be kept:\n$smt" }
    }
}
