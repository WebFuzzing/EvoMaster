package org.evomaster.core.solver

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * A boolean literal in a WHERE clause is not a column, even though the grammar cannot tell them apart.
 *
 * `SqlBooleanLiteralValue` only ever arrives from CHECK constraints; in a query, an unquoted `true`
 * reaches the translator as a column name. Emitting it as one produces a field selector over the row
 * constant, which Z3 rejects outright — and that rejection costs a full round-trip to the container,
 * so it is both wrong and expensive. On one system under test, 24 of 77 solver calls failed this way.
 *
 * The spelling asserted below is the one the CHECK-constraint path already uses and the one
 * `SMTLibZ3DbConstraintSolver.toBoolean` reads back, so the value survives the round trip into a
 * `BooleanGene`.
 */
class BooleanLiteralTranslationTest {

    private val mapper = ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private fun schema(): DbInfoDto = mapper.readValue(
        javaClass.getResourceAsStream("/solver/sample-schema.json")!!.bufferedReader().readText(),
        DbInfoDto::class.java
    )

    private fun generate(query: String): String =
        SmtLibGenerator(schema(), 1).generateSMT(CCJSqlParserUtil.parse(query)).toString()

    @Test
    fun `a true literal is encoded as a boolean value, not as a column reference`() {
        val smt = generate("SELECT ID FROM ACCOUNT WHERE ACTIVE = true")

        assertTrue(smt.contains("\"True\"")) { "expected the boolean encoding in:\n$smt" }
        assertFalse(Regex("""\(\s*[Tt][Rr][Uu][Ee]\s+\w""").containsMatchIn(smt)) {
            "'true' was emitted as a field selector over a row constant, which Z3 rejects:\n$smt"
        }
    }

    @Test
    fun `a false literal is encoded as a boolean value`() {
        val smt = generate("SELECT ID FROM ACCOUNT WHERE ACTIVE = false")

        assertTrue(smt.contains("\"False\"")) { "expected the boolean encoding in:\n$smt" }
        assertFalse(Regex("""\(\s*[Ff][Aa][Ll][Ss][Ee]\s+\w""").containsMatchIn(smt)) {
            "'false' was emitted as a field selector over a row constant:\n$smt"
        }
    }

    @Test
    fun `the literal is recognised whatever its case`() {
        listOf("TRUE", "True", "true").forEach { spelling ->
            val smt = generate("SELECT ID FROM ACCOUNT WHERE ACTIVE = $spelling")
            assertTrue(smt.contains("\"True\"")) { "'$spelling' was not recognised as a literal" }
        }
    }

    /**
     * The guard must not swallow real columns. A qualified name is always a column reference, and an
     * ordinary column keeps being translated as one.
     */
    @Test
    fun `ordinary columns are still translated as columns`() {
        val smt = generate("SELECT ID FROM ACCOUNT a WHERE a.ACTIVE = true AND a.ID > 0")

        assertTrue(smt.contains("\"True\"")) { "the literal should still be encoded:\n$smt" }
        assertTrue(smt.contains("ACTIVE") || smt.contains("active")) {
            "the qualified column reference disappeared:\n$smt"
        }
    }
}
