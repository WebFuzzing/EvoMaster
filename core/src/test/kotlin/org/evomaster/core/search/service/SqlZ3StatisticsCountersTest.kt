package org.evomaster.core.search.service

import org.evomaster.core.search.service.Statistics.SqlZ3TranslationFailure
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The solver counters are only worth reading if they add up, since they are reported side by side in
 * the statistics file and a reader will subtract one from another. Two relations have to hold for that
 * to be sound, and neither is enforced by the types.
 */
class SqlZ3StatisticsCountersTest {

    @Test
    fun `the failure breakdown adds up to the aggregate`() {
        val s = Statistics()

        repeat(3) { s.reportSqlZ3ParseFailure(SqlZ3TranslationFailure.SQL_PARSE) }
        repeat(5) { s.reportSqlZ3ParseFailure(SqlZ3TranslationFailure.SMTLIB_GENERATION) }

        assertEquals(3, s.getSqlZ3SqlParseFailureCount())
        assertEquals(5, s.getSqlZ3SmtlibGenFailureCount())
        assertEquals(
            s.getSqlZ3SqlParseFailureCount() + s.getSqlZ3SmtlibGenFailureCount(),
            s.getSqlZ3ParseFailureCount(),
            "the breakdown is reported next to the aggregate, so a reader can subtract one from the other"
        )
    }

    /**
     * A translation failure is a resolved query, not an unresolved one: reporting it has to close the
     * memoization accounting the same way a solver answer does, or seen would exceed hits plus misses
     * by exactly the number of failures.
     */
    @Test
    fun `a translation failure counts as a cache miss`() {
        val s = Statistics()

        s.reportSqlZ3QuerySeen("select 1".hashCode())
        s.reportSqlZ3ParseFailure(SqlZ3TranslationFailure.SQL_PARSE)

        s.reportSqlZ3QuerySeen("select 1".hashCode())
        s.reportSqlZ3CacheHit()

        assertEquals(2, s.getSqlZ3QueriesSeenCount())
        assertEquals(1, s.getSqlZ3CacheHitCount())
        assertEquals(1, s.getSqlZ3CacheMissCount())
        assertEquals(
            s.getSqlZ3QueriesSeenCount(),
            s.getSqlZ3CacheHitCount() + s.getSqlZ3CacheMissCount(),
            "every query seen has to end up classified as either a hit or a miss"
        )
    }

    @Test
    fun `solve time accumulates across calls`() {
        val s = Statistics()

        assertEquals(0L, s.getSqlZ3SolveTimeMs())
        s.reportSqlZ3SolveTime(120)
        s.reportSqlZ3SolveTime(80)

        assertEquals(200L, s.getSqlZ3SolveTimeMs())
    }

    /**
     * Insertion time is reported per execution because the average is the quantity of interest: rows a
     * solver produced once are re-inserted on every later evaluation that carries them, so a total
     * without a count cannot distinguish a few slow insertions from many cheap ones.
     */
    @Test
    fun `insertion time is accumulated together with its execution count`() {
        val s = Statistics()

        assertEquals(0, s.getSqlInsertionExecutionCount())
        assertEquals(0L, s.getSqlInsertionExecutionTimeMs())

        s.reportSqlInsertionExecution(30)
        s.reportSqlInsertionExecution(70)
        s.reportSqlInsertionExecution(0)

        assertEquals(3, s.getSqlInsertionExecutionCount())
        assertEquals(100L, s.getSqlInsertionExecutionTimeMs())
    }

    /**
     * The reported duration comes from a finally block, so a failed insertion still yields a sample.
     * A zero-length one has to be counted like any other, otherwise the average is computed over a
     * denominator that silently drops the fast cases.
     */
    @Test
    fun `a zero duration is still one execution`() {
        val s = Statistics()

        s.reportSqlInsertionExecution(0)

        assertEquals(1, s.getSqlInsertionExecutionCount())
        assertEquals(0L, s.getSqlInsertionExecutionTimeMs())
    }
}
