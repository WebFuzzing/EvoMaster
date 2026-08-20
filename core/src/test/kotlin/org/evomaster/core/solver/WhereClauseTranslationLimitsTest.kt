package org.evomaster.core.solver

import org.evomaster.dbconstraint.ConstraintDatabaseType
import org.evomaster.dbconstraint.parser.jsql.JSqlConditionParser
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

/**
 * Pins what a WHERE clause is allowed to contain on its way to the solver, and in particular the two
 * ways a condition can still be lost.
 *
 * Both matter because the consequence is the same and is silent: the constraint never reaches Z3,
 * the weakened formula stays satisfiable, the solver answers `SAT`, and the rows it produces need
 * not satisfy the query that triggered the call. Nothing downstream distinguishes those rows from
 * correct ones.
 *
 * The two paths differ in whether anything notices:
 *
 *  - A condition the parser rejects raises an exception, which `SmtLibGenerator` catches while
 *    discarding **the entire WHERE clause** — not only the sub-expression it could not read. This
 *    increments the partial-translation counter, so it is at least visible in the statistics.
 *  - A condition the parser accepts but the visitor cannot translate is dropped by returning an
 *    empty node. No exception, no counter, no trace.
 */
class WhereClauseTranslationLimitsTest {

    private val parser = JSqlConditionParser()

    private fun parse(where: String) = parser.parse(where, ConstraintDatabaseType.H2)

    /**
     * Timestamp literals are converted to whole epoch seconds, so a parsed condition carries the
     * epoch as a literal. Comparing against it checks the conversion, not merely that parsing
     * succeeded — a formatter can accept a string and still read it wrongly.
     */
    private fun assertEpoch(where: String, expected: Long) {
        val rendered = assertDoesNotThrow("expected '$where' to parse") { parse(where) }.toString()
        assertTrue(rendered.contains(expected.toString())) {
            "expected '$where' to yield epoch $expected, got: $rendered"
        }
    }

    /**
     * The layouts a database or an ORM emits in practice: with or without an ISO `T`, with or
     * without seconds, with any fractional precision, and with a trailing offset.
     *
     * Before these were accepted, each one raised an exception that discarded the whole WHERE
     * clause. Sub-second precision is the common case, since it appears whenever a query compares a
     * column against the current instant.
     */
    @Test
    fun `timestamp literals are accepted in the layouts databases emit`() {
        assertEpoch("T > TIMESTAMP '2026-08-18 06:25:20'", 1787034320)
        assertEpoch("T > TIMESTAMP '2026-08-18T06:25:20'", 1787034320)
        assertEpoch("T > TIMESTAMP '2026-08-18 06:25:20.3'", 1787034320)
        assertEpoch("T > TIMESTAMP '2026-08-18 06:25:20.351'", 1787034320)
        assertEpoch("T > TIMESTAMP '2026-08-18 06:25:20.351123'", 1787034320)
        assertEpoch("T > TIMESTAMP '2026-08-18 06:25:20.351123456'", 1787034320)
        assertEpoch("T > TIMESTAMP '2026-08-18 06:25'", 1787034300)
        assertEpoch("T > TIMESTAMP '2026-08-18'", 1787011200)
    }

    /** An explicit offset is honoured rather than ignored, in both spellings. */
    @Test
    fun `timestamp offsets are honoured`() {
        assertEpoch("T > TIMESTAMP '2026-08-18 06:25:20+02:00'", 1787027120)
        assertEpoch("T > TIMESTAMP '2026-08-18 06:25:20+02'", 1787027120)
        assertEpoch("T > TIMESTAMP '2026-08-18 06:25:20Z'", 1787034320)
    }

    /** Everyday conditions parse, so any remaining limitation is specific rather than general. */
    @Test
    fun `ordinary conditions are accepted`() {
        listOf(
            "ID = 42",
            "NAME = 'alice'",
            "ACTIVE = TRUE",
            "SCORE >= 4.5",
            "LEVEL > 3 AND LEVEL <= 10",
            "ID IN (1, 2, 3)",
            "NAME LIKE 'a%'",
            "LOWER(NAME) LIKE 'a%'",
            "UPPER(NAME) = 'ALICE'",
            "RANK < 50 OR TITLE = 'draft'"
        ).forEach { where -> assertDoesNotThrow("expected '$where' to parse") { parse(where) } }
    }

    /**
     * Null checks parse cleanly, which is precisely why their loss is invisible: they are discarded
     * later, by the visitor, without raising anything and without touching any counter. Should
     * `SMTConditionVisitor` learn to translate them, this test keeps documenting where the boundary
     * used to be.
     */
    @Test
    fun `null checks parse but are dropped later without a trace`() {
        listOf(
            "VALID_TO IS NULL",
            "VALID_TO IS NOT NULL",
            "VALID_TO IS NULL OR N > 3"
        ).forEach { where -> assertDoesNotThrow("expected '$where' to parse") { parse(where) } }
    }
}
