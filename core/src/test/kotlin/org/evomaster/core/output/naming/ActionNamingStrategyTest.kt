package org.evomaster.core.output.naming

import org.evomaster.core.TestUtils
import org.evomaster.core.output.Termination
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.*
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Solution
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*


class ActionNamingStrategyTest {

    @Test
    fun testGetOnRootPathIsIncludedInName() {
        val eIndividual = getEvaluatedIndividualWith("/")

        val solution = Solution(Collections.singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, Collections.emptyList(), Collections.emptyList())

        val namingStrategy = ActionTestCaseNamingStrategy(solution)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_root_returns_200", testCases[0].name)
    }

    @Test
    fun testGetOnItemsPathIsIncludedInName() {
        val eIndividual = getEvaluatedIndividualWith("/items")

        val solution = Solution(Collections.singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, Collections.emptyList(), Collections.emptyList())

        val namingStrategy = ActionTestCaseNamingStrategy(solution)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_items_returns_200", testCases[0].name)
    }

    private fun getEvaluatedIndividualWith(path: String): EvaluatedIndividual<RestIndividual> {
        val sampleType = SampleType.RANDOM
        val action = RestCallAction("1", HttpVerb.GET, RestPath(path), mutableListOf())
        val restActions = listOf(action).toMutableList()
        val individual = RestIndividual(restActions, sampleType)
        TestUtils.doInitializeIndividualForTesting(individual)

        val fitnessVal = FitnessValue(0.0)
        val result = RestCallResult(action.getLocalId())
        result.setStatusCode(200)
        val results = listOf(result)
        return EvaluatedIndividual<RestIndividual>(fitnessVal, individual, results)
    }
}
