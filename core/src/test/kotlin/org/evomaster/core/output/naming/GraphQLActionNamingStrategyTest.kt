package org.evomaster.core.output.naming

import com.webfuzzing.commons.faults.FaultCategory
import org.evomaster.core.TestUtils
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.problem.enterprise.DetectedFault
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.graphql.GQMethodType
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.problem.graphql.GraphQlCallResult
import org.evomaster.core.problem.graphql.param.GQInputParam
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Solution
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Collections.singletonList


class GraphQLActionNamingStrategyTest {

    companion object {
        val pythonFormatter = LanguageConventionFormatter(OutputFormat.PYTHON_UNITTEST)
        const val MAX_NAME_LENGTH = 80
    }

    @Test
    fun testMutationOnAddReturnsEmpty() {
        val eIndividual = getEvaluatedIndividualWith(GQMethodType.MUTATION)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = GraphQLActionTestCaseNamingStrategy(solution, pythonFormatter, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0_mutation_on_add_returns_empty", testCases[0].name)
    }

    @Test
    fun testQueryOnAddReturnsData() {
        val eIndividual = getEvaluatedIndividualWith(GQMethodType.QUERY)
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = GraphQLActionTestCaseNamingStrategy(solution, pythonFormatter, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0_query_on_add_returns_empty", testCases[0].name)
    }

    @Test
    fun testQueryOnAddCausesInternalServerError() {
        val eIndividual = getEvaluatedIndividualWithFaults(GQMethodType.QUERY, singletonList(DetectedFault(FaultCategory.HTTP_STATUS_500, "items")))
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = GraphQLActionTestCaseNamingStrategy(solution, pythonFormatter, MAX_NAME_LENGTH)
        val testCases = namingStrategy.getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0_query_on_add_causes500_internalServerError", testCases[0].name)
    }

    @Test
    fun testActionAndMethodNameAreAddedTogether() {
        val eIndividual = getEvaluatedIndividualWithFaults(GQMethodType.QUERY, singletonList(DetectedFault(FaultCategory.HTTP_STATUS_500, "items")))
        val solution = Solution(singletonList(eIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val withActionAndMethodName = GraphQLActionTestCaseNamingStrategy(solution, pythonFormatter, 20).getTestCases()
        val noActionAndMethodName = GraphQLActionTestCaseNamingStrategy(solution, pythonFormatter, 15).getTestCases()

        assertEquals(1, withActionAndMethodName.size)
        assertEquals(1, noActionAndMethodName.size)
        assertEquals(withActionAndMethodName[0].test, noActionAndMethodName[0].test)
        assertEquals("test_0_query_on_add", withActionAndMethodName[0].name)
        assertEquals("test_0", noActionAndMethodName[0].name)
    }

    private fun getEvaluatedIndividualWith(query: GQMethodType): EvaluatedIndividual<GraphQLIndividual> {
        return getEvaluatedIndividualWithFaults(query, emptyList())
    }

    private fun getEvaluatedIndividualWithFaults(query: GQMethodType, faults: List<DetectedFault>): EvaluatedIndividual<GraphQLIndividual> {
        val sampleType = SampleType.RANDOM
        val op = OptionalGene("foo", StringGene("foo", "foo\""))
        val param = GQInputParam("foo", op)
        val action = GraphQLAction("Mutation:add", "add", query, mutableListOf(param))

        val actions = mutableListOf<EnterpriseActionGroup<*>>()
        actions.add(EnterpriseActionGroup(mutableListOf(action),GraphQLAction::class.java))
        val individual = GraphQLIndividual(sampleType, actions)
        TestUtils.doInitializeIndividualForTesting(individual)

        val fitnessVal = FitnessValue(0.0)
        val result = GraphQlCallResult(action.getLocalId())
        faults.forEach { fault -> result.addFault(fault) }
        val results = listOf(result)
        return EvaluatedIndividual(fitnessVal, individual, results)
    }
}
