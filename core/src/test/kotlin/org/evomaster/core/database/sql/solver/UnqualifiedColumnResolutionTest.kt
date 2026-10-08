package org.evomaster.core.database.sql.solver

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * In a JOIN, an unqualified column belongs to whichever joined table declares it.
 *
 * It used to resolve to a single default table, the first element of the hash set returned by
 * JSqlParser's TablesNamesFinder, which is not necessarily the table in FROM. The constraint then
 * selected the column from the wrong row -- or one that table does not have, which Z3 rejects.
 */
class UnqualifiedColumnResolutionTest {

    private val mapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private fun schema(): DbInfoDto = mapper.readValue(
        javaClass.getResourceAsStream("/solver/sample-schema.json")!!.bufferedReader().readText(),
        DbInfoDto::class.java
    )

    private fun generate(query: String): String =
        SmtLibGenerator(schema(), 1).generateSMT(CCJSqlParserUtil.parse(query)).toString()

    private fun selects(smt: String, column: String, table: String) =
        Regex("""\(\s*$column\s+${table}__1\s*\)""", RegexOption.IGNORE_CASE).containsMatchIn(smt)

    @Test
    fun `an unqualified column resolves to the joined table that declares it`() {
        // Each column exists in only one of the two tables, so both are resolved whichever comes first
        val smt = generate(
            "SELECT p.ID FROM PROJECT p JOIN ACCOUNT a ON p.ACCOUNT_ID = a.ID WHERE TITLE = 'x' AND LEVEL = 3"
        )

        assertTrue(selects(smt, "TITLE", "project")) { "TITLE should be selected from PROJECT:\n$smt" }
        assertTrue(selects(smt, "LEVEL", "account")) { "LEVEL should be selected from ACCOUNT:\n$smt" }
        assertFalse(selects(smt, "TITLE", "account")) { "TITLE was selected from ACCOUNT:\n$smt" }
        assertFalse(selects(smt, "LEVEL", "project")) { "LEVEL was selected from PROJECT:\n$smt" }
    }

    @Test
    fun `the same holds with the tables joined the other way round`() {
        val smt = generate(
            "SELECT a.ID FROM ACCOUNT a JOIN PROJECT p ON p.ACCOUNT_ID = a.ID WHERE TITLE = 'x' AND LEVEL = 3"
        )

        assertTrue(selects(smt, "TITLE", "project")) { "TITLE should be selected from PROJECT:\n$smt" }
        assertTrue(selects(smt, "LEVEL", "account")) { "LEVEL should be selected from ACCOUNT:\n$smt" }
    }

    @Test
    fun `a column declared by several joined tables falls back to the table in FROM`() {
        // ID exists in both tables; SQL would reject it as ambiguous, so any choice is a fallback, but
        // it must not depend on hash-set order
        val smt = generate("SELECT p.TITLE FROM PROJECT p JOIN ACCOUNT a ON p.ACCOUNT_ID = a.ID WHERE ID = 5")

        assertTrue(selects(smt, "ID", "project")) { "ID should fall back to PROJECT:\n$smt" }
    }
}
