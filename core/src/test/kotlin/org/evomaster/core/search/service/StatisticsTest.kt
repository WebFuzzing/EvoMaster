package org.evomaster.core.search.service

import org.junit.Test
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

}
