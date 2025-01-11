package org.evomaster.core.output

import org.evomaster.core.output.naming.NumberedTestCaseNamingStrategy
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getEvaluatedIndividualWith
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getRestCallAction
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.search.Solution
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class TestSuiteOrganizerTest {

    @Test
    fun sortedByPathSegmentFirst() {
        val noPathSegmentInd = getEvaluatedIndividualWith(getRestCallAction("/"))
        val onePathSegmentInd = getEvaluatedIndividualWith(getRestCallAction("/organization"))
        val twoPathSegmentsInd = getEvaluatedIndividualWith(getRestCallAction("/organization/{name}"))
        val individuals = mutableListOf(noPathSegmentInd, onePathSegmentInd, twoPathSegmentsInd)
        individuals.shuffle()
        val solution = Solution(individuals, "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val sortedTestCases = SortingHelper().sort(solution, NumberedTestCaseNamingStrategy(solution))

        assertEquals(sortedTestCases[0].test, noPathSegmentInd)
        assertEquals(sortedTestCases[1].test, onePathSegmentInd)
        assertEquals(sortedTestCases[2].test, twoPathSegmentsInd)
    }

    @Test
    fun sortedByStatusCodeWhenEqualPathSegmentSize() {
        val status200Ind = getEvaluatedIndividualWith(getRestCallAction("/organization"), 200)
        val status401Ind = getEvaluatedIndividualWith(getRestCallAction("/organization"), 401)
        val status500Ind = getEvaluatedIndividualWith(getRestCallAction("/organization"), 500)
        val individuals = mutableListOf(status200Ind, status401Ind, status500Ind)
        individuals.shuffle()
        val solution = Solution(individuals, "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val sortedTestCases = SortingHelper().sort(solution, NumberedTestCaseNamingStrategy(solution))

        assertEquals(sortedTestCases[0].test, status500Ind)
        assertEquals(sortedTestCases[1].test, status200Ind)
        assertEquals(sortedTestCases[2].test, status401Ind)
    }

    @Test
    fun sortedByMethodWhenEqualPathSegmentsAndStatusCode() {
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

        val sortedTestCases = SortingHelper().sort(solution, NumberedTestCaseNamingStrategy(solution))

        assertEquals(sortedTestCases[0].test, getInd)
        assertEquals(sortedTestCases[1].test, postInd)
        assertEquals(sortedTestCases[2].test, putInd)
        assertEquals(sortedTestCases[3].test, deleteInd)
        assertEquals(sortedTestCases[4].test, optionsInd)
        assertEquals(sortedTestCases[5].test, patchInd)
        assertEquals(sortedTestCases[6].test, traceInd)
        assertEquals(sortedTestCases[7].test, headInd)
    }

}
