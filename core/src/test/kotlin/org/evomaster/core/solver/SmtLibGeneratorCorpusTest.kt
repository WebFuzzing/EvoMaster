package org.evomaster.core.solver

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import net.sf.jsqlparser.JSQLParserException
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.Statement
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.evomaster.core.database.sql.solver.SmtLibGenerator
import org.evomaster.core.database.sql.solver.SMTLibZ3DbConstraintSolver
import org.evomaster.core.database.sql.solver.SMTConditionVisitor

/**
 * Pushes a corpus of SELECT statements through the two steps that [SMTLibZ3DbConstraintSolver.solve]
 * performs before reaching Z3 — parsing the SQL, then generating SMT-LIB — and classifies what
 * happens to each one.
 *
 * Why this is worth a test. Both of those steps report failure through the *same* statistics
 * counter, so an aggregate figure such as "half of all solver invocations failed" cannot say whether
 * the SQL parser or the SMT-LIB generator is responsible, and therefore cannot direct a fix. Running
 * the two steps separately over a fixed corpus does distinguish them, and pins the result so a
 * regression in either component is visible.
 *
 * The corpus and schema live in `src/test/resources/solver/`. They are synthetic, but written in the
 * shape an ORM emits, and cover the constructs the visitor has to handle: equality and comparison,
 * `IN`, `LIKE`, `LOWER`/`UPPER`, `IS NULL`, conjunction, disjunction, timestamps, and joins across
 * one and two hops.
 */
class SmtLibGeneratorCorpusTest {

    companion object {
        private const val SCHEMA = "/solver/sample-schema.json"
        private const val QUERIES = "/solver/sample-queries.sql"

        /** Matches the default of `sqlZ3NumberOfRows`. */
        private const val NUMBER_OF_ROWS = 1

        private const val EXPECTED_TABLES = 5
        private const val EXPECTED_QUERIES = 26

        /** See the doc on `partial translations are pinned`. */
        private const val EXPECTED_PARTIAL = 1
    }

    /** Outcome of pushing one query through the two steps. */
    private enum class Outcome { PARSE_FAILURE, GENERATION_FAILURE, PARTIAL_TRANSLATION, TRANSLATED }

    private data class Result(val query: String, val outcome: Outcome, val detail: String = "")

    private fun loadSchema(): DbInfoDto {
        val json = javaClass.getResourceAsStream(SCHEMA)!!.bufferedReader().readText()
        return ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .readValue(json, DbInfoDto::class.java)
    }

    private fun loadQueries(): List<String> =
        javaClass.getResourceAsStream(QUERIES)!!.bufferedReader().readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("--") }

    /**
     * Mirrors [SMTLibZ3DbConstraintSolver.parseStatement], including its retry on a sanitized query.
     * Kept in sync deliberately: the point is to reproduce the production path, not to approximate it.
     */
    private fun parseStatement(sqlQuery: String): Statement =
        try {
            CCJSqlParserUtil.parse(sqlQuery)
        } catch (_: JSQLParserException) {
            CCJSqlParserUtil.parse(sqlQuery.replace("local temporary", ""))
        }

    private fun classify(schema: DbInfoDto, sqlQuery: String): Result {
        val statement = try {
            parseStatement(sqlQuery)
        } catch (e: Exception) {
            return Result(sqlQuery, Outcome.PARSE_FAILURE, e.javaClass.simpleName)
        }

        val generator = SmtLibGenerator(schema, NUMBER_OF_ROWS)
        try {
            generator.generateSMT(statement)
        } catch (e: RuntimeException) {
            return Result(sqlQuery, Outcome.GENERATION_FAILURE, e.message ?: e.javaClass.simpleName)
        }

        return if (generator.skippedQueryConstraints > 0)
            Result(sqlQuery, Outcome.PARTIAL_TRANSLATION, "${generator.skippedQueryConstraints} dropped")
        else
            Result(sqlQuery, Outcome.TRANSLATED)
    }

    private fun runCorpus(): List<Result> {
        val schema = loadSchema()
        return loadQueries().map { classify(schema, it) }
    }

    @Test
    fun `corpus is loaded`() {
        assertEquals(EXPECTED_TABLES, loadSchema().tables.size)
        assertEquals(EXPECTED_QUERIES, loadQueries().size)
    }

    /**
     * Table declarations are emitted for the whole schema before any query-specific work, and every
     * column type in this schema is one the generator maps. A failure here would therefore have to
     * come from the query, not from the schema.
     */
    @Test
    fun `no query fails during SMT-LIB generation`() {
        val failures = runCorpus().filter { it.outcome == Outcome.GENERATION_FAILURE }
        assertTrue(failures.isEmpty()) {
            "SMT-LIB generation failed for ${failures.size} of $EXPECTED_QUERIES queries:\n" +
                failures.joinToString("\n") { "  ${it.detail}\n    ${it.query.take(160)}" }
        }
    }

    @Test
    fun `no query fails during SQL parsing`() {
        val failures = runCorpus().filter { it.outcome == Outcome.PARSE_FAILURE }
        assertTrue(failures.isEmpty()) {
            "SQL parsing failed for ${failures.size} of $EXPECTED_QUERIES queries:\n" +
                failures.joinToString("\n") { "  ${it.detail}\n    ${it.query.take(160)}" }
        }
    }

    /**
     * A table whose name carries a non-ASCII character still translates.
     *
     * SMT-LIB symbols are ASCII, so table names are folded before being used as identifiers, which
     * leaves the same table reachable under two spellings: the schema's own and the folded one. The
     * qualifier check in `SMTConditionVisitor` compares against the schema spelling, so it is not
     * affected — this test exists to keep it that way, since folding both sides instead would make
     * two names that differ only by an accent indistinguishable, and let a genuinely absent table
     * through the check.
     */
    @Test
    fun `a table whose name is not ASCII still translates`() {
        val schema = loadSchema()
        // NOTE is a leaf: renaming a table that other tables reference by foreign key would fail for
        // an unrelated reason.
        val accented = "ANOTACIÓN"
        val table = schema.tables.first { it.id.name == "NOTE" }
        table.id.name = accented
        table.columns.forEach { it.table = accented }

        listOf(
            "SELECT s.ID FROM $accented s WHERE s.ID = 42",   // calificada: ortografía del esquema
            "SELECT ID FROM $accented WHERE ID = 42"          // sin calificar: llega ya plegada
        ).forEach { query ->
            val result = classify(schema, query)
            assertTrue(result.outcome != Outcome.GENERATION_FAILURE) {
                "expected '$query' to translate, but generation failed: ${result.detail}"
            }
        }
    }

    /**
     * Characterisation test, not a requirement. It pins how much of the corpus is translated with a
     * constraint silently discarded — a case that matters because the weakened formula usually
     * remains satisfiable, so the solver still answers SAT and the rows it produces need not satisfy
     * the original WHERE clause. Nothing downstream can tell the two apart.
     *
     * The one query counted here joins a real table against a derived table. The join condition
     * refers to the derived side, which has no declared SMT constant, so that condition is dropped
     * while the rest of the query is still constrained. Dropping it is the correct outcome: emitting
     * a reference to an undeclared constant instead produced a formula Z3 rejects, spending a full
     * round-trip to learn nothing.
     *
     * Two queries in the corpus used to be partial. Both carry sub-second precision in a timestamp
     * literal:
     *
     *     WHERE note0_.VALID_TO > TIMESTAMP '2026-01-01 00:00:00.351'
     *
     * The condition parser once accepted exactly one layout, `yyyy-MM-dd HH:mm:ss`, and raised on
     * everything else — whereupon the *entire* WHERE clause was discarded, not just the comparison it
     * could not read. Since an ORM emits sub-second literals whenever it compares a column against
     * the current instant, this was routine. The accepted layouts were widened; see
     * [WhereClauseTranslationLimitsTest].
     *
     * Note what this counter does *not* see. `IS NULL` and `IS NOT NULL` parse successfully, but
     * `SMTConditionVisitor` emits an empty node for them, so the condition is dropped without any
     * exception and without touching the counter. The corpus contains such a query and it is
     * classified here as fully translated, which it is not. Zero partial translations therefore
     * means "no condition was lost *loudly*", not "no condition was lost".
     */
    @Test
    fun `partial translations are pinned`() {
        val results = runCorpus()
        val byOutcome = results.groupingBy { it.outcome }.eachCount()
        val partial = byOutcome[Outcome.PARTIAL_TRANSLATION] ?: 0
        val translated = byOutcome[Outcome.TRANSLATED] ?: 0

        assertEquals(EXPECTED_QUERIES, partial + translated) { "unexpected outcomes: $byOutcome" }
        assertEquals(EXPECTED_PARTIAL, partial) {
            "expected $EXPECTED_PARTIAL partial translations, got $partial:\n" +
                results.filter { it.outcome == Outcome.PARTIAL_TRANSLATION }
                    .joinToString("\n") { "  ${it.detail}: ${it.query.take(200)}" }
        }
    }
}
