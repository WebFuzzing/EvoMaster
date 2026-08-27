package org.evomaster.core.solver

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import net.sf.jsqlparser.JSQLParserException
import net.sf.jsqlparser.parser.CCJSqlParserUtil
import net.sf.jsqlparser.statement.Statement
import org.evomaster.client.java.controller.api.dto.database.schema.DbInfoDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.evomaster.core.database.sql.solver.SmtLibGenerator
import org.evomaster.core.database.sql.solver.service.SMTLibZ3DbConstraintSolver
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
     * A table whose name carries a non-ASCII character translates in full.
     *
     * An unqualified column resolves against the default table, whose name reaches the visitor already
     * ASCII-folded. Checking that folded name against the schema's own spelling rejects a table that
     * is perfectly valid, and the failure is quiet: the condition is dropped and the query still
     * reports a partial translation rather than an error.
     */
    @Test
    fun `a table whose name is not ASCII translates in full`() {
        val schema = loadSchema()
        val accented = "ANOTACIÓN"
        val table = schema.tables.first { it.id.name == "NOTE" }
        table.id.name = accented
        table.columns.forEach { it.table = accented }

        val result = classify(schema, "SELECT ID FROM $accented WHERE ID = 42")

        assertEquals(Outcome.TRANSLATED, result.outcome) {
            "the condition was dropped for a table that exists: ${result.detail}"
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
    fun `a derived table never costs the whole query`() {
        val results = runCorpus()
        val byOutcome = results.groupingBy { it.outcome }.eachCount()
        val partial = byOutcome[Outcome.PARTIAL_TRANSLATION] ?: 0
        val translated = byOutcome[Outcome.TRANSLATED] ?: 0

        assertEquals(EXPECTED_QUERIES, partial + translated) { "unexpected outcomes: $byOutcome" }

        val derived = results.filter { it.query.contains("UNION ALL") }
        assertTrue(derived.isNotEmpty()) { "the corpus should carry at least one derived-table query" }

        // The guarantee is that a derived table never costs the whole query. What happens to the
        // individual conditions differs: one that references the derived alias has to be dropped,
        // while a query that merely selects from it has nothing to drop and translates in full.
        derived.forEach {
            assertNotEquals(Outcome.GENERATION_FAILURE, it.outcome) {
                "a derived table discarded the whole query (${it.detail}): ${it.query.take(160)}"
            }
        }
        assertTrue(derived.any { it.outcome == Outcome.PARTIAL_TRANSLATION }) {
            "the corpus should exercise a condition over a derived table, which has to be dropped"
        }
    }

    /**
     * What the containment is worth, stated as an invariant rather than a count.
     *
     * The query below joins a real table against a derived one and puts three conditions in the same
     * `WHERE`: two over the real table, one over the derived alias. Only the third is untranslatable.
     *
     * Before the clause was split into conjuncts, the exception raised by the third discarded all
     * three, because the caller guards the clause as a unit. The assertion here is that the two
     * translatable conditions survive: their columns must appear in the generated formula, and
     * exactly one condition must be recorded as lost.
     */
    @Test
    fun `an untranslatable condition costs only itself`() {
        val query = "SELECT p.ID FROM PROJECT p" +
            " LEFT OUTER JOIN (SELECT ID, PROJECT_ID FROM LABEL UNION ALL SELECT ID, PROJECT_ID FROM LABEL) l" +
            " ON p.ID = l.PROJECT_ID" +
            " WHERE p.TITLE = 'kept' AND l.ID = 7 AND p.RANK > 3"

        val schema = loadSchema()
        val generator = SmtLibGenerator(schema, NUMBER_OF_ROWS)
        val smt = generator.generateSMT(parseStatement(query)).toString()

        /*
            Two conditions reference the derived alias — the JOIN's ON and the middle conjunct of the
            WHERE — and each is dropped on its own. What matters is that the count equals the number
            of untranslatable conditions, not the number of clauses that contained one.
         */
        assertEquals(2, generator.skippedQueryConstraints) {
            "only the conditions over the derived table should be lost"
        }
        listOf(
            "(assert (= ${ref("TITLE", "project")} \"kept\"))",
            "(assert (> ${ref("RANK", "project")} 3))"
        ).forEach { expected ->
            assertTrue(smt.contains(expected)) { "$expected should have survived:\n$smt" }
        }
    }

    /** Left outer join of a real table against a derived one, the shape an ORM emits for inheritance. */
    private val joinWithDerived =
        "SELECT p.ID FROM PROJECT p" +
            " LEFT OUTER JOIN (SELECT ID, PROJECT_ID FROM LABEL) l ON p.ID = l.PROJECT_ID"

    /*
        The assertions below name the exact text a translated condition takes, rather than matching a
        column anywhere in the formula. Two things would otherwise make them pass for the wrong
        reason: a column name also appears in its datatype declaration, and PROJECT.RANK carries a
        CHECK constraint that emits "(<= (RANK project__1) 100)" whatever the query says.
     */

    /** Text of the reference to [column] of the first row of [table]. */
    private fun ref(column: String, table: String) = "($column ${table}__1)"

    private fun generate(query: String): Pair<SmtLibGenerator, String> {
        val generator = SmtLibGenerator(loadSchema(), NUMBER_OF_ROWS)
        return generator to generator.generateSMT(parseStatement(query)).toString()
    }

    /**
     * The counterpart of the previous test: the split stops at `AND`.
     *
     * Pruning the two operators is not equally safe. Dropping a conjunct leaves a formula weaker than
     * the query, so Z3 may return rows that do not satisfy what was dropped, which is the failure
     * mode this code already accepts and counts. Dropping a disjunct would leave a formula
     * *stronger* than the query, which can turn a satisfiable query into `unsat` and yield no rows
     * at all. So a disjunction is translated whole or not at all.
     *
     * Here the disjunction has one side over a derived table. Neither side may reach the formula:
     * keeping `p.TITLE = 'x'` alone would demand of every generated row something the query only
     * offered as an alternative. The conjunct beside it is unaffected.
     */
    @Test
    fun `a disjunction is translated whole or not at all`() {
        val (_, smt) = generate("$joinWithDerived WHERE (l.ID = 7 OR p.TITLE = 'x') AND p.RANK > 3")

        assertFalse(smt.contains(ref("TITLE", "project"))) {
            "the translatable side of the disjunction must not be asserted on its own:\n$smt"
        }
        assertTrue(smt.contains("(assert (> ${ref("RANK", "project")} 3))")) {
            "the conjunct beside the disjunction should have survived:\n$smt"
        }
    }

    /**
     * The same rule reached from the other side: a disjunction every operand of which translates is
     * emitted as one `or`, and the conjunct beside it is emitted separately. Without this, the test
     * above would also pass on an implementation that simply dropped every disjunction it saw.
     */
    @Test
    fun `a disjunction that translates in full is kept`() {
        val (_, smt) = generate("$joinWithDerived WHERE (p.TITLE = 'x' OR p.RANK > 3) AND p.RANK < 90")

        assertTrue(smt.contains("(or (= ${ref("TITLE", "project")}")) {
            "the disjunction should be emitted whole:\n$smt"
        }
        assertTrue(smt.contains("(assert (< ${ref("RANK", "project")} 90))")) {
            "the conjunct beside it should be emitted separately:\n$smt"
        }
    }

    /**
     * The subtle case, and the reason [flattening][SmtLibGenerator] cannot simply recurse through
     * every operator it meets.
     *
     * The `AND` here sits *inside* a disjunction. Splitting it would leave
     * `p.TITLE = 'x' OR p.RANK > 3` — a formula that demands of every row something the query only
     * offered as one of two alternatives, and which can be `unsat` where the query was satisfiable.
     * So the clause is dropped whole instead.
     */
    @Test
    fun `a conjunction inside a disjunction is not split`() {
        val (_, smt) = generate("$joinWithDerived WHERE p.TITLE = 'x' OR (l.ID = 7 AND p.RANK > 3)")

        assertFalse(smt.contains(ref("TITLE", "project"))) {
            "the disjunction must not be pruned from inside:\n$smt"
        }
        assertFalse(smt.contains("(assert (> ${ref("RANK", "project")} 3))")) {
            "the disjunction must not be pruned from inside:\n$smt"
        }
    }

    /**
     * A characterisation test, not a guarantee. It pins behaviour this change neither introduces nor
     * repairs, so that a later fix has to face it deliberately.
     *
     * `SMTConditionVisitor` drops the operands of a disjunction that translate to nothing and keeps
     * the rest. `IS NULL` translates to nothing, so `A OR (X IS NULL)` reaches the formula as `A`
     * alone: every row is now required to satisfy `A`, which the query only offered as one
     * alternative. This is the opposite direction of loss from everything else here, and no counter
     * records it — `skippedQueryConstraints` stays at the value the `ON` clause alone accounts for.
     *
     * It belongs to the untracked `NULL` gap. Splitting the clause per conjunct does not reach it,
     * because the loss happens inside the visitor and below the level the split operates on.
     */
    @Test
    fun `a disjunction silently loses an operand that translates to nothing`() {
        val (generator, smt) = generate("$joinWithDerived WHERE p.TITLE IS NULL OR p.RANK > 3")

        assertTrue(smt.contains("(assert (> ${ref("RANK", "project")} 3))")) {
            "the surviving operand is asserted on its own, which is stronger than the query:\n$smt"
        }
        assertFalse(smt.contains("(or (> ${ref("RANK", "project")}")) {
            "no disjunction is left of the WHERE clause:\n$smt"
        }
        assertEquals(1, generator.skippedQueryConstraints) {
            "the loss is not counted: the only skipped constraint should be the ON over the derived table"
        }
    }

    /**
     * The case that motivates reading the query's aliases rather than matching against the schema
     * alone.
     *
     * Here the sub-select is aliased `LABEL`, which is also the name of a real table in the schema.
     * Deciding by name — "is this qualifier one of the schema's tables?" — answers yes, and the
     * generator emits a constraint selecting `TITLE` from a `LABEL` row. `LABEL` has no such column,
     * so the formula reaches Z3 only to be rejected, and nothing records that a condition was lost.
     *
     * Reading the query's own `FROM` settles it: the alias is declared there as a sub-select, so the
     * condition is dropped and counted instead.
     */
    @Test
    fun `a derived table aliased as a schema table is still derived`() {
        val query = "SELECT sub.TITLE FROM (SELECT TITLE FROM PROJECT) LABEL WHERE LABEL.TITLE = 'x'"

        val result = classify(loadSchema(), query)

        assertEquals(Outcome.PARTIAL_TRANSLATION, result.outcome) {
            "the condition over the derived table should be dropped and counted, not emitted" +
                " against the schema table that happens to share its name (was: ${result.detail})"
        }
    }
}
