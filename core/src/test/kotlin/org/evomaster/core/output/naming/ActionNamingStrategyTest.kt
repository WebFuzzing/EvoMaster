package org.evomaster.core.output.naming

import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.TestUtils
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.problem.enterprise.DetectedFault
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.*
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Solution
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Collections.singletonList


class ActionNamingStrategyTest {

    @Test
    fun testGetOnRootPathIsIncludedInName() {
        val eIndividual = getEvaluatedIndividualWith("/")
        val languageConventionFormatter = LanguageConventionFormatter(OutputFormat.PYTHON_UNITTEST)

        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = ActionTestCaseNamingStrategy(solution, languageConventionFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_root_returns_200", testCases[0].name)
    }

    @Test
    fun testGetOnItemsPathIsIncludedInName() {
        val eIndividual = getEvaluatedIndividualWith("/items")
        val languageConventionFormatter = LanguageConventionFormatter(OutputFormat.PYTHON_UNITTEST)

        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = ActionTestCaseNamingStrategy(solution, languageConventionFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_items_returns_200", testCases[0].name)
    }

    @Test
    fun test500ResponseNamedWithInternalServerError() {
        val eIndividual = getEvaluatedIndividualWithFaults("/items", 500, singletonList(DetectedFault(FaultCategory.HTTP_STATUS_500, "items")))
        val languageConventionFormatter = LanguageConventionFormatter(OutputFormat.PYTHON_UNITTEST)

        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = ActionTestCaseNamingStrategy(solution, languageConventionFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_items_causes500_internalServerError", testCases[0].name)
    }

    @Test
    fun testResponseNamedWithMultipleFaults() {
        val faults = listOf(DetectedFault(FaultCategory.GQL_ERROR_FIELD, "items"), DetectedFault(FaultCategory.HTTP_INVALID_LOCATION, "items"), DetectedFault(FaultCategory.HTTP_STATUS_500, "items"))
        val eIndividual = getEvaluatedIndividualWithFaults("/items", 500, faults)
        val languageConventionFormatter = LanguageConventionFormatter(OutputFormat.PYTHON_UNITTEST)

        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = ActionTestCaseNamingStrategy(solution, languageConventionFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(1, testCases.size)
        assertEquals("test_0_GET_on_items_showsFaults_100_102_301", testCases[0].name)
    }

    private fun getEvaluatedIndividualWith(path: String, statusCode: Int = 200): EvaluatedIndividual<RestIndividual> {
        return getEvaluatedIndividualWithFaults(path, statusCode, emptyList())
    }

    private fun getEvaluatedIndividualWithFaults(path: String, statusCode: Int, faults: List<DetectedFault>): EvaluatedIndividual<RestIndividual> {
        val sampleType = SampleType.RANDOM
        val action = RestCallAction("1", HttpVerb.GET, RestPath(path), mutableListOf())
        val restActions = listOf(action).toMutableList()
        val individual = RestIndividual(restActions, sampleType)
        TestUtils.doInitializeIndividualForTesting(individual)

        val fitnessVal = FitnessValue(0.0)
        val result = RestCallResult(action.getLocalId())
        result.setStatusCode(statusCode)
        faults.forEach { fault -> result.addFault(fault) }
        val results = listOf(result)
        return EvaluatedIndividual<RestIndividual>(fitnessVal, individual, results)
    }
}
