package org.evomaster.core.output

import org.evomaster.core.TestUtils
import org.evomaster.core.output.naming.NumberedTestCaseNamingStrategy
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getEvaluatedIndividualWith
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getRestCallAction
import org.evomaster.core.output.sorting.SortingStrategy
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.enterprise.EnterpriseActionGroup
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.graphql.GQMethodType
import org.evomaster.core.problem.graphql.GraphQLAction
import org.evomaster.core.problem.graphql.GraphQLIndividual
import org.evomaster.core.problem.graphql.GraphQlCallResult
import org.evomaster.core.problem.graphql.param.GQInputParam
import org.evomaster.core.problem.rest.data.HttpVerb
import org.evomaster.core.problem.rpc.RPCCallAction
import org.evomaster.core.problem.rpc.RPCIndividual
import org.evomaster.core.problem.rpc.param.RPCParam
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.FitnessValue
import org.evomaster.core.search.Solution
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID

class TestSuiteOrganizerTest {

    companion object {
        val sortingStrategy = SortingStrategy.TARGET_INCREMENTAL
    }

    @Test
    fun restSortedByPathSegmentFirst() {
        val noPathSegmentInd = getEvaluatedIndividualWith(getRestCallAction("/"))
        val onePathSegmentInd = getEvaluatedIndividualWith(getRestCallAction("/organization"))
        val twoPathSegmentsInd = getEvaluatedIndividualWith(getRestCallAction("/organization/{name}"))
        val individuals = mutableListOf(noPathSegmentInd, onePathSegmentInd, twoPathSegmentsInd)
        individuals.shuffle()
        val solution = Solution(individuals, "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val sortedTestCases = SortingHelper().sort(solution, NumberedTestCaseNamingStrategy(solution), sortingStrategy)

        assertEquals(sortedTestCases[0].test, noPathSegmentInd)
        assertEquals(sortedTestCases[1].test, onePathSegmentInd)
        assertEquals(sortedTestCases[2].test, twoPathSegmentsInd)
    }

    @Test
    fun restSortedByStatusCodeWhenEqualPathSegmentSize() {
        val status200Ind = getEvaluatedIndividualWith(getRestCallAction("/organization"), 200)
        val status401Ind = getEvaluatedIndividualWith(getRestCallAction("/organization"), 401)
        val status500Ind = getEvaluatedIndividualWith(getRestCallAction("/organization"), 500)
        val individuals = mutableListOf(status200Ind, status401Ind, status500Ind)
        individuals.shuffle()
        val solution = Solution(individuals, "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val sortedTestCases = SortingHelper().sort(solution, NumberedTestCaseNamingStrategy(solution), sortingStrategy)

        assertEquals(sortedTestCases[0].test, status500Ind)
        assertEquals(sortedTestCases[1].test, status200Ind)
        assertEquals(sortedTestCases[2].test, status401Ind)
    }

    @Test
    fun restSortedByMethodWhenEqualPathSegmentsAndStatusCode() {
        val getInd = getEvaluatedIndividualWith(getRestCallAction("/organization", HttpVerb.GET))
        val postInd = getEvaluatedIndividualWith(getRestCallAction("/organization", HttpVerb.POST))
        val putInd = getEvaluatedIndividualWith(getRestCallAction("/organization", HttpVerb.PUT))
        val deleteInd = getEvaluatedIndividualWith(getRestCallAction("/organization", HttpVerb.DELETE))
        val optionsInd = getEvaluatedIndividualWith(getRestCallAction("/organization", HttpVerb.OPTIONS))
        val patchInd = getEvaluatedIndividualWith(getRestCallAction("/organization", HttpVerb.PATCH))
        val traceInd = getEvaluatedIndividualWith(getRestCallAction("/organization", HttpVerb.TRACE))
        val headInd = getEvaluatedIndividualWith(getRestCallAction("/organization", HttpVerb.HEAD))
        val individuals = mutableListOf(getInd, postInd, putInd, deleteInd, optionsInd, patchInd, traceInd, headInd)
        individuals.shuffle()
        val solution = Solution(individuals, "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val sortedTestCases = SortingHelper().sort(solution, NumberedTestCaseNamingStrategy(solution), sortingStrategy)

        assertEquals(sortedTestCases[0].test, getInd)
        assertEquals(sortedTestCases[1].test, postInd)
        assertEquals(sortedTestCases[2].test, putInd)
        assertEquals(sortedTestCases[3].test, deleteInd)
        assertEquals(sortedTestCases[4].test, optionsInd)
        assertEquals(sortedTestCases[5].test, patchInd)
        assertEquals(sortedTestCases[6].test, traceInd)
        assertEquals(sortedTestCases[7].test, headInd)
    }

    @Test
    fun graphSortedByMethodNameFirst() {
        val aLetterIndividual = getGraphQLEvaluatedIndividual(GQMethodType.QUERY, "aLetterMethod")
        val bLetterIndividual = getGraphQLEvaluatedIndividual(GQMethodType.QUERY, "bLetterMethod")
        val cLetterIndividual = getGraphQLEvaluatedIndividual(GQMethodType.QUERY, "cLetterMethod")
        val individuals = mutableListOf(aLetterIndividual, bLetterIndividual, cLetterIndividual)
        individuals.shuffle()
        val solution = Solution(individuals, "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val sortedTestCases = SortingHelper().sort(solution, NumberedTestCaseNamingStrategy(solution), sortingStrategy)

        assertEquals(sortedTestCases[0].test, aLetterIndividual)
        assertEquals(sortedTestCases[1].test, bLetterIndividual)
        assertEquals(sortedTestCases[2].test, cLetterIndividual)
    }

    @Test
    fun graphSortedByMethodTypeWhenEqualMethodNameAndParametersSize() {
        val paramLambda = { name: String, value: String -> getGraphQLParam(name, value) }
        val queryOneParameterIndividual = getGraphQLEvaluatedIndividual(GQMethodType.QUERY, "myMethod", getListOfParams(1, paramLambda))
        val queryTwoParametersIndividual = getGraphQLEvaluatedIndividual(GQMethodType.QUERY, "myMethod", getListOfParams(2, paramLambda))
        val mutationOneParameterIndividual = getGraphQLEvaluatedIndividual(GQMethodType.MUTATION, "myMethod", getListOfParams(1, paramLambda))
        val mutationTwoParametersIndividual = getGraphQLEvaluatedIndividual(GQMethodType.MUTATION, "myMethod", getListOfParams(2, paramLambda))
        val individuals = mutableListOf(queryOneParameterIndividual, queryTwoParametersIndividual, mutationOneParameterIndividual, mutationTwoParametersIndividual)
        individuals.shuffle()
        val solution = Solution(individuals, "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val sortedTestCases = SortingHelper().sort(solution, NumberedTestCaseNamingStrategy(solution), sortingStrategy)

        assertEquals(sortedTestCases[0].test, queryOneParameterIndividual)
        assertEquals(sortedTestCases[1].test, queryTwoParametersIndividual)
        assertEquals(sortedTestCases[2].test, mutationOneParameterIndividual)
        assertEquals(sortedTestCases[3].test, mutationTwoParametersIndividual)
    }

    @Test
    fun graphSortedByAmountOfParametersWhenEqualMethodNameAndType() {
        val paramLambda = { name: String, value: String -> getGraphQLParam(name, value) }
        val oneParameterIndividual = getGraphQLEvaluatedIndividual(GQMethodType.QUERY, "myMethod", getListOfParams(1, paramLambda))
        val twoParametersIndividual = getGraphQLEvaluatedIndividual(GQMethodType.QUERY, "myMethod", getListOfParams(2, paramLambda))
        val threeParametersIndividual = getGraphQLEvaluatedIndividual(GQMethodType.QUERY, "myMethod", getListOfParams(3, paramLambda))
        val individuals = mutableListOf(oneParameterIndividual, twoParametersIndividual, threeParametersIndividual)
        individuals.shuffle()
        val solution = Solution(individuals, "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val sortedTestCases = SortingHelper().sort(solution, NumberedTestCaseNamingStrategy(solution), sortingStrategy)

        assertEquals(sortedTestCases[0].test, oneParameterIndividual)
        assertEquals(sortedTestCases[1].test, twoParametersIndividual)
        assertEquals(sortedTestCases[2].test, threeParametersIndividual)
    }

    @Test
    fun rpcSortedByClassNameFirst() {
        val aFakeClassIndividual = getRPCEvaluatedIndividual("aFakeInterfaceClass:fakeInterfaceMethod")
        val bFakeClassIndividual = getRPCEvaluatedIndividual("bFakeInterfaceClass:fakeInterfaceMethod")
        val cFakeClassIndividual = getRPCEvaluatedIndividual("cFakeInterfaceClass:fakeInterfaceMethod")
        val individuals = mutableListOf(aFakeClassIndividual, bFakeClassIndividual, cFakeClassIndividual)
        individuals.shuffle()
        val solution = Solution(individuals, "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val sortedTestCases = SortingHelper().sort(solution, NumberedTestCaseNamingStrategy(solution), sortingStrategy)

        assertEquals(sortedTestCases[0].test, aFakeClassIndividual)
        assertEquals(sortedTestCases[1].test, bFakeClassIndividual)
        assertEquals(sortedTestCases[2].test, cFakeClassIndividual)
    }

    @Test
    fun rpcSortedByMethodNameWhenEqualClassName() {
        val aFakeMethodIndividual = getRPCEvaluatedIndividual("fakeInterfaceClass:aFakeInterfaceMethod")
        val bFakeMethodIndividual = getRPCEvaluatedIndividual("fakeInterfaceClass:bFakeInterfaceMethod")
        val cFakeMethodIndividual = getRPCEvaluatedIndividual("fakeInterfaceClass:cFakeInterfaceMethod")
        val individuals = mutableListOf(aFakeMethodIndividual, bFakeMethodIndividual, cFakeMethodIndividual)
        individuals.shuffle()
        val solution = Solution(individuals, "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val sortedTestCases = SortingHelper().sort(solution, NumberedTestCaseNamingStrategy(solution), sortingStrategy)

        assertEquals(sortedTestCases[0].test, aFakeMethodIndividual)
        assertEquals(sortedTestCases[1].test, bFakeMethodIndividual)
        assertEquals(sortedTestCases[2].test, cFakeMethodIndividual)
    }

    @Test
    fun rpcSortedByAmountOfParametersWhenClassAndMethodNames() {
        val paramLambda = { name: String, value: String -> getRPCParam(name, value) }
        val oneParameterIndividual = getRPCEvaluatedIndividual("fakeInterfaceClass:fakeInterfaceMethod", getListOfParams(1, paramLambda))
        val twoParametersIndividual = getRPCEvaluatedIndividual("fakeInterfaceClass:fakeInterfaceMethod", getListOfParams(2, paramLambda))
        val threeParametersIndividual = getRPCEvaluatedIndividual("fakeInterfaceClass:fakeInterfaceMethod", getListOfParams(3, paramLambda))
        val individuals = mutableListOf(oneParameterIndividual, twoParametersIndividual, threeParametersIndividual)
        individuals.shuffle()
        val solution = Solution(individuals, "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val sortedTestCases = SortingHelper().sort(solution, NumberedTestCaseNamingStrategy(solution), sortingStrategy)

        assertEquals(sortedTestCases[0].test, oneParameterIndividual)
        assertEquals(sortedTestCases[1].test, twoParametersIndividual)
        assertEquals(sortedTestCases[2].test, threeParametersIndividual)
    }

    private fun getGraphQLEvaluatedIndividual(query: GQMethodType, methodName: String, params: MutableList<Param> = mutableListOf()): EvaluatedIndividual<GraphQLIndividual> {
        val action = GraphQLAction(methodName, methodName, query, params)

        val actions = mutableListOf<EnterpriseActionGroup<*>>()
        actions.add(EnterpriseActionGroup(mutableListOf(action), GraphQLAction::class.java))

        val individual = GraphQLIndividual(SampleType.RANDOM, actions)
        TestUtils.doInitializeIndividualForTesting(individual)

        val results = listOf(GraphQlCallResult(action.getLocalId()))
        return EvaluatedIndividual(FitnessValue(0.0), individual, results)
    }

    private fun getListOfParams(amount: Int, paramFunction: (name: String, value: String) -> Param): MutableList<Param> {
        val params = mutableListOf<Param>()
        for (i in 0..amount) {
            val name = UUID.randomUUID().toString()
            val value = UUID.randomUUID().toString()
            params.add(paramFunction(name, value))
        }
        return params
    }

    private fun getGraphQLParam(name: String, value: String): GQInputParam {
        val op = OptionalGene(name, StringGene(name, value))
        return GQInputParam(name, op)
    }

    private fun getRPCEvaluatedIndividual(interfaceId: String, params: MutableList<Param> = mutableListOf()): EvaluatedIndividual<RPCIndividual> {
        val action = RPCCallAction(interfaceId, "${interfaceId}_0", params, null, null)
        val externalAction = EvaluatedIndividualBuilder.buildFakeDbExternalServiceAction(1).plus(EvaluatedIndividualBuilder.buildFakeRPCExternalServiceAction(1))

        val individual = RPCIndividual(SampleType.RANDOM, actions=mutableListOf(action), externalServicesActions = mutableListOf(externalAction))
        TestUtils.doInitializeIndividualForTesting(individual)

        val results = listOf(GraphQlCallResult(action.getLocalId()))
        return EvaluatedIndividual(FitnessValue(0.0), individual, results)
    }

    private fun getRPCParam(name: String, value: String): RPCParam {
        val op = OptionalGene(name, StringGene(name, value))
        return RPCParam(name, op)
    }


}
