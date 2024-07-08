package org.evomaster.core.search.service

import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class StatisticsTest {

    @Test
    fun testMongoHeuristicAverage() {
        val statistics = Statistics()
        statistics.reportNumberOfEvaluatedDocumentsForComputingMongoHeuristic(10)
        statistics.reportNumberOfEvaluatedDocumentsForComputingMongoHeuristic(20)
        statistics.reportNumberOfEvaluatedDocumentsForComputingMongoHeuristic(30)

        assertEquals(3, statistics.mongoHeuristicEvaluationCount)
        assertEquals((10 + 20 + 30).toDouble() / 3, statistics.averageNumberOfEvaluatedDocumentsForMongoHeuristic())
    }
}
