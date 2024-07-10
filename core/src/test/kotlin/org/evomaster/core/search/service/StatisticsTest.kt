package org.evomaster.core.search.service

import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class StatisticsTest {

    @Test
    fun testMongoHeuristicsAverage() {
        val statistics = Statistics()
        statistics.reportNumberOfEvaluatedDocumentsForComputingMongoHeuristic(10)
        statistics.reportNumberOfEvaluatedDocumentsForComputingMongoHeuristic(20)
        statistics.reportNumberOfEvaluatedDocumentsForComputingMongoHeuristic(30)

        assertEquals(3, statistics.mongoHeuristicsEvaluationCount)
        assertEquals((10 + 20 + 30).toDouble() / 3, statistics.averageNumberOfEvaluatedDocumentsForMongoHeuristics())
    }

    @Test
    fun testSqlHeuristicsAverage() {
        val statistics = Statistics()
        statistics.reportNumberOfEvaluatedRowsForComputingSqlHeuristic(5)
        statistics.reportNumberOfEvaluatedRowsForComputingSqlHeuristic(7)
        statistics.reportNumberOfEvaluatedRowsForComputingSqlHeuristic(11)
        statistics.reportNumberOfEvaluatedRowsForComputingSqlHeuristic(13)

        assertEquals(4, statistics.sqlHeuristicsEvaluationCount)
        assertEquals((5 + 7 + 11+13).toDouble() / 4, statistics.averageNumberOfEvaluatedRowsForSqlHeuristics())
    }

}
