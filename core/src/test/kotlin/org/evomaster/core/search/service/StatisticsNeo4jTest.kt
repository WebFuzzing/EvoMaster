package org.evomaster.core.search.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class StatisticsNeo4jTest {

    @Test
    fun testNeo4jHeuristicsAverage() {
        val statistics = Statistics()
        statistics.reportNumberOfEvaluatedNodesForNeo4jHeuristic(10)
        statistics.reportNumberOfEvaluatedNodesForNeo4jHeuristic(20)
        statistics.reportNumberOfEvaluatedNodesForNeo4jHeuristic(30)

        repeat(3) {
            statistics.reportNeo4jHeuristicEvaluationSuccess()
        }

        assertEquals(3, statistics.getNeo4jHeuristicsEvaluationCount())
        assertEquals((10 + 20 + 30).toDouble() / 3, statistics.averageNumberOfEvaluatedNodesForNeo4jHeuristics())
    }
}
