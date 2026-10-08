package org.evomaster.core.search.service

import io.mockk.mockk
import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Individual
import org.evomaster.core.search.action.ActionResult
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PostProcessingObservationTest {
    private class Fitness : FitnessFunction<Individual>() {
        val result = RestCallResult("a", true).apply { setTcpProblem(true); setBody("captured") }
        var throwAfterObservation = false

        override fun doCalculateCoverage(individual: Individual, targets: Set<Int>, allTargets: Boolean,
                                         fullyCovered: Boolean, descriptiveIds: Boolean): EvaluatedIndividual<Individual>? {
            observePostProcessingResults(listOf(result))
            result.setBody("changed after snapshot")
            if (throwAfterObservation) throw IllegalStateException("coverage unavailable")
            return null
        }

        fun emitOutsideEvaluation() = observePostProcessingResults(listOf(result))
    }

    @Test
    fun `null coverage retains independent response snapshot without changing the return value`() {
        val fitness = Fitness()
        var captured: List<ActionResult> = emptyList()
        var calls = 0
        assertNull(fitness.computeWholeAchievedCoverageForPostProcessing(mockk()) { captured = it; calls++ })
        val result = captured.single() as RestCallResult
        assertTrue(result.getTcpProblem())
        assertTrue(result.stopping)
        assertEquals("captured", result.getBody())
        fitness.emitOutsideEvaluation()
        assertNull(fitness.computeWholeAchievedCoverageForPostProcessing(mockk()))
        assertEquals(1, calls)
    }

    @Test
    fun `observer is removed even when evaluation throws`() {
        val fitness = Fitness().apply { throwAfterObservation = true }
        var calls = 0
        assertThrows(IllegalStateException::class.java) {
            fitness.computeWholeAchievedCoverageForPostProcessing(mockk()) { calls++ }
        }
        fitness.emitOutsideEvaluation()
        assertEquals(1, calls)
    }
}
