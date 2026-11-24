package org.evomaster.core.search.service

import io.mockk.*
import org.evomaster.core.output.EvaluatedIndividualBuilder
import org.evomaster.core.database.sql.SqlAction
import org.evomaster.core.problem.rest.data.*
import org.evomaster.core.search.Solution
import org.evomaster.core.search.service.time.ExecutionPhaseController
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** Verify that one lifecycle restart precedes the entire repeated-detection batch. */
class FlakinessDetectorTest {

    private fun inject(target: Any, name: String, value: Any) {
        target.javaClass.getDeclaredField(name).apply { isAccessible = true }.set(target, value)
    }

    @Test
    fun restartOnceBeforeRepeatedDetectionPreservesTheHistoricalBaseline() {
        val action = RestCallAction("1", HttpVerb.GET, RestPath("/contacts/28"), mutableListOf())
        val (_, _, original) = EvaluatedIndividualBuilder.buildResourceEvaluatedIndividual(
            mutableListOf(), mutableListOf(mutableListOf<SqlAction>() to mutableListOf(action)))
        val baseline = original.seeResult(action.getLocalId()) as RestCallResult
        baseline.setTimedout(false)
        baseline.setStatusCode(204)
        val fresh = original.copy()
        (fresh.seeResult(action.getLocalId()) as RestCallResult).setStatusCode(404)
        val archive = mockk<Archive<RestIndividual>>()
        val solution = mockk<Solution<RestIndividual>>()
        every { solution.individuals } returns mutableListOf(original)
        every { archive.extractSolution() } returns solution
        val fitness = mockk<FitnessFunction<RestIndividual>>()
        var restarted = false
        every { fitness.prepareForFlakinessDetection() } answers { restarted = true }
        every { fitness.computeWholeAchievedCoverageForPostProcessing(any()) } answers {
            assertTrue(restarted)
            fresh
        }
        val epc = mockk<ExecutionPhaseController>()
        every { epc.hasPhaseTimedOut(any()) } returns false
        val detector = FlakinessDetector<RestIndividual>()
        inject(detector, "archive", archive)
        inject(detector, "fitness", fitness)
        inject(detector, "epc", epc)
        inject(detector, "statistics", mockk<Statistics>(relaxed = true))
        detector.reexecuteToDetectFlakiness(5)
        verify(exactly = 1) { fitness.prepareForFlakinessDetection() }
        verify(exactly = 5) { fitness.computeWholeAchievedCoverageForPostProcessing(original.individual) }
        assertEquals(204, baseline.getStatusCode())
        assertEquals(5, baseline.getFlakyObservations().size)

        detector.reexecuteToDetectFlakiness(0)
        verify(exactly = 1) { fitness.prepareForFlakinessDetection() }
    }
}
