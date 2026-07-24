package org.evomaster.core.search.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals

class StatisticsTest {

    @Test
    fun testMongoHeuristicsAverage() {
        val statistics = Statistics()
        statistics.reportNumberOfEvaluatedDocumentsForMongoHeuristic(10)
        statistics.reportNumberOfEvaluatedDocumentsForMongoHeuristic(20)
        statistics.reportNumberOfEvaluatedDocumentsForMongoHeuristic(30)

        repeat (3) {
            statistics.reportMongoHeuristicEvaluationSuccess()
        }

        assertEquals(3, statistics.getMongoHeuristicsEvaluationCount())
        assertEquals((10 + 20 + 30).toDouble() / 3, statistics.averageNumberOfEvaluatedDocumentsForMongoHeuristics())
    }

    @Test
    fun testSqlHeuristicsAverage() {
        val statistics = Statistics()
        statistics.reportNumberOfEvaluatedRowsForSqlHeuristic(5)
        statistics.reportNumberOfEvaluatedRowsForSqlHeuristic(7)
        statistics.reportNumberOfEvaluatedRowsForSqlHeuristic(11)
        statistics.reportNumberOfEvaluatedRowsForSqlHeuristic(13)

        repeat (4) {
            statistics.reportSqlHeuristicEvaluationSuccess()
        }

        assertEquals(4, statistics.getSqlHeuristicsEvaluationCount())
        assertEquals((5 + 7 + 11 + 13).toDouble() / 4, statistics.averageNumberOfEvaluatedRowsForSqlHeuristics())
    }

    @Test
    fun testSqlZ3CacheAccountingInvariant() {
        val statistics = Statistics()

        // Query #1: first sighting -> cache miss, solved as SAT
        statistics.reportSqlZ3QuerySeen(1)
        statistics.reportSqlZ3Sat(10)

        // Query #2: first sighting -> cache miss, solved as UNSAT
        statistics.reportSqlZ3QuerySeen(2)
        statistics.reportSqlZ3Unsat(5)

        // Query #1 seen again -> served from the memoization cache (a hit, not a miss)
        statistics.reportSqlZ3QuerySeen(1)
        statistics.reportSqlZ3CacheHit()

        assertEquals(3, statistics.getSqlZ3QueriesSeenCount())
        assertEquals(1, statistics.getSqlZ3CacheHitCount())
        assertEquals(2, statistics.getSqlZ3CacheMissCount())

        // Every solve() is either a cache hit or a miss.
        assertEquals(
            statistics.getSqlZ3QueriesSeenCount(),
            statistics.getSqlZ3CacheHitCount() + statistics.getSqlZ3CacheMissCount()
        )
    }

}
