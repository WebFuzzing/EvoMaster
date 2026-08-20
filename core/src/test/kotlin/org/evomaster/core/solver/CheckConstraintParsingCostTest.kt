package org.evomaster.core.solver

import org.evomaster.dbconstraint.ConstraintDatabaseType
import org.evomaster.dbconstraint.parser.jsql.JSqlConditionParser
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.evomaster.core.database.sql.solver.SmtLibGenerator

/**
 * Times [JSqlConditionParser] on the shapes of CHECK constraint a PostgreSQL schema actually
 * carries, rather than the simple numeric bounds used elsewhere in the tests.
 *
 * Why this exists. An application with a large PostgreSQL schema was observed spending hundreds of
 * seconds per query inside SMT-LIB generation. Schema size does not explain it — generation is
 * linear and a schema thirty times larger completes in milliseconds, see
 * [SmtLibGeneratorScalingTest] — so the cost had to come from a specific expression.
 *
 * It did. An ORM emits, for every enum column on PostgreSQL, a constraint of the form
 *
 *     ((status)::text = ANY ((ARRAY['A'::character varying, 'B'::character varying])::text[]))
 *
 * and the dialect transform ahead of the parser used to mangle it: its ARRAY rule ran to the last
 * `]` in the string, which belongs to the `::text[]` cast rather than to the array, so JSQLParser was
 * handed SQL with unbalanced brackets. It backtracked over that for a very long time and then threw.
 * The work was both expensive and futile — `SmtLibGenerator` caught the exception and produced no
 * constraint at all. On one real schema, 44 of 48 constraints failed this way, together costing over
 * five minutes, with a single 24 KB constraint accounting for 275 seconds of it.
 *
 * With the transform corrected the same 48 constraints parse in 72 ms in total, and — the part that
 * matters beyond speed — they are now actually applied instead of silently dropped.
 *
 * These tests are the regression guard. A transform that breaks the shape again does not fail
 * visibly; it just gets slow and quietly stops constraining anything.
 */
class CheckConstraintParsingCostTest {

    private val parser = JSqlConditionParser()

    private data class Timing(val label: String, val ms: Long, val outcome: String)

    private fun time(label: String, expression: String): Timing {
        val start = System.nanoTime()
        val outcome = try {
            parser.parse(expression, ConstraintDatabaseType.POSTGRES)
            "parsed"
        } catch (e: Exception) {
            e.javaClass.simpleName
        }
        return Timing(label, (System.nanoTime() - start) / 1_000_000, outcome)
    }

    /** The shape an ORM emits for an enum column on PostgreSQL. */
    private fun enumCheck(values: Int): String {
        val array = (1..values).joinToString(", ") { "'VALUE_$it'::character varying" }
        return "((status)::text = ANY ((ARRAY[$array])::text[]))"
    }

    @Test
    fun `ordinary constraints parse cheaply`() {
        val ordinary = listOf(
            time("simple IN, 3 values", "(status IN ('A', 'B', 'C'))"),
            time("conjunction of 4 bounds", "(a >= 0 AND a <= 10 AND b >= 0 AND b <= 10)"),
            time("nested parens x20", "(".repeat(20) + "a > 0" + ")".repeat(20)),
            time("long disjunction, 32 terms", (1..32).joinToString(" OR ") { "(x = $it)" })
        )
        ordinary.forEach { t ->
            assertTrue(t.outcome == "parsed") { "expected ${t.label} to parse, got ${t.outcome}" }
        }
    }

    /**
     * The enum shape parses, at every width a real schema is likely to carry.
     *
     * This is the assertion that matters most: a constraint that fails to parse is discarded, so the
     * generated rows are free to violate it. Failing here means the solver is once again producing
     * data the database will reject.
     */
    @Test
    fun `postgres enum constraints parse at realistic widths`() {
        listOf(8, 32, 200).map { time("$it enum values", enumCheck(it)) }.forEach { t ->
            assertTrue(t.outcome == "parsed") {
                "expected ${t.label} to parse, got ${t.outcome} — the dialect transform is probably " +
                    "mangling the ::text[] cast again, which drops the constraint entirely"
            }
        }
    }

    /**
     * And parses cheaply. Absolute timings are machine dependent, so the bound is deliberately loose:
     * the regression being guarded against is three orders of magnitude, not a few milliseconds. The
     * 200-value constraint below is about 7 KB, a width at which the old transform cost 25 seconds.
     */
    @Test
    fun `a wide enum constraint does not cost seconds`() {
        // Warm up: the first parse of a run pays class loading and JIT, which would otherwise
        // dominate and make the measurement meaningless.
        repeat(3) { time("warmup", "(a >= 0 AND a <= 10)") }

        val baseline = time("ordinary conjunction", "(a >= 0 AND a <= 10 AND b >= 0 AND b <= 10)")
        val wide = time("200 enum values", enumCheck(200))

        println(
            "\nJSqlConditionParser on PostgreSQL enum CHECK constraints\n" +
                String.format("  %6d ms  %-10s %s%n", baseline.ms, baseline.outcome, baseline.label) +
                String.format("  %6d ms  %-10s %s", wide.ms, wide.outcome, wide.label)
        )

        assertTrue(wide.ms < 2_000) {
            "expected a 7 KB enum constraint to parse quickly, but it took ${wide.ms}ms; before the " +
                "dialect transform was corrected this shape cost about 25 seconds at this width"
        }
    }
}
