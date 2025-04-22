package org.evomaster.core.output.naming

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.output.naming.RestActionTestCaseUtils.ensureGeneValue
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getBooleanQueryParam
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getEvaluatedIndividualWith
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getIntegerQueryParam
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getRestCallAction
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getStringQueryParam
import org.evomaster.core.output.naming.rest.RestActionTestCaseNamingStrategy
import org.evomaster.core.search.Solution
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Collections.singletonList

class RestTestCaseNameLengthTest {

    companion object {
        val javaFormatter = LanguageConventionFormatter(OutputFormat.JAVA_JUNIT_4)
        const val QUERY_PARAMS_IN_NAME = true
    }

    @Test
    fun simpleTestWithShortNameOnlyIncludesTestNumber() {
        val simpleIndividual = getEvaluatedIndividualWith(getRestCallAction("/languages"))

        val solution = Solution(mutableListOf(simpleIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val testCases = RestActionTestCaseNamingStrategy(solution, javaFormatter, QUERY_PARAMS_IN_NAME, 10).getTestCases()

        assertEquals(1, testCases.size)
        assertEquals("test_0", testCases[0].name)
    }

    @Test
    fun pathIsFavouredInsteadOfResult() {
        val simpleIndividual = getEvaluatedIndividualWith(getRestCallAction("/languages"))

        val solution = Solution(mutableListOf(simpleIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val longTestCases = RestActionTestCaseNamingStrategy(solution, javaFormatter, QUERY_PARAMS_IN_NAME, 35).getTestCases()
        val shortTestCases = RestActionTestCaseNamingStrategy(solution, javaFormatter, QUERY_PARAMS_IN_NAME, 30).getTestCases()

        assertEquals(1, longTestCases.size)
        assertEquals(1, shortTestCases.size)
        assertEquals(shortTestCases[0].test, longTestCases[0].test)
        assertEquals("test_0_getOnLanguagesReturnsEmpty", longTestCases[0].name)
        assertEquals("test_0_getOnLanguages", shortTestCases[0].name)
    }

    @Test
    fun pathAmbiguitySolverIsFavouredInsteadOfQueryParams() {
        val syntaxLanguagesIndividual = getEvaluatedIndividualWith(getRestCallAction("/languages"))
        val syntaxLanguagesIndividualWithQP = getEvaluatedIndividualWith(getRestCallAction("/syntax/languages", parameters = singletonList(getStringQueryParam("myQueryParam"))))
        ensureGeneValue(syntaxLanguagesIndividualWithQP, "myQueryParam", "aStringValue")

        val solution = Solution(mutableListOf(syntaxLanguagesIndividual, syntaxLanguagesIndividualWithQP), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val longTestCases = RestActionTestCaseNamingStrategy(solution, javaFormatter, QUERY_PARAMS_IN_NAME, 80).getTestCases()
        val shortTestCases = RestActionTestCaseNamingStrategy(solution, javaFormatter, QUERY_PARAMS_IN_NAME, 30).getTestCases()

        assertEquals(2, longTestCases.size)
        assertEquals(2, shortTestCases.size)
        assertEquals(shortTestCases[1].test, longTestCases[1].test)
        assertEquals("test_1_getOnSyntaxLanguagesWithQueryParamReturnsEmpty", longTestCases[1].name)
        assertEquals("test_1_getOnSyntaxLanguages", shortTestCases[1].name)
    }

    @Test
    fun queryParamsDisambiguationRespectsBooleanNumberStringOrder() {
        val syntaxLanguagesIndividual = getEvaluatedIndividualWith(getRestCallAction("/syntax/languages"))
        val syntaxLanguagesIndividual2 = getEvaluatedIndividualWith(getRestCallAction("/syntax/languages", parameters = mutableListOf(getBooleanQueryParam("firstParam"), getStringQueryParam("secondParam"), getIntegerQueryParam("thirdParam"))))
        ensureGeneValue(syntaxLanguagesIndividual2, "firstParam", "true")
        ensureGeneValue(syntaxLanguagesIndividual2, "secondParam", "")
        ensureGeneValue(syntaxLanguagesIndividual2, "thirdParam", "-1")

        val solution = Solution(mutableListOf(syntaxLanguagesIndividual, syntaxLanguagesIndividual2), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val fullNames = RestActionTestCaseNamingStrategy(solution, javaFormatter, QUERY_PARAMS_IN_NAME, 100).getTestCases()
        val noParamsNames = RestActionTestCaseNamingStrategy(solution, javaFormatter, QUERY_PARAMS_IN_NAME, 50).getTestCases()
        val onlyBooleanNames = RestActionTestCaseNamingStrategy(solution, javaFormatter, QUERY_PARAMS_IN_NAME, 65).getTestCases()
        val booleanAndIntegerNames = RestActionTestCaseNamingStrategy(solution, javaFormatter, QUERY_PARAMS_IN_NAME, 85).getTestCases()

        assertEquals(2, fullNames.size)
        assertEquals(2, noParamsNames.size)
        assertEquals(2, onlyBooleanNames.size)
        assertEquals(2, booleanAndIntegerNames.size)
        assertEquals(fullNames[1].test, noParamsNames[1].test)
        assertEquals(fullNames[1].test, onlyBooleanNames[1].test)
        assertEquals(fullNames[1].test, booleanAndIntegerNames[1].test)
        assertEquals("test_1_getOnSyntaxLanguagesWithQueryParamsFirstParamNegativeThirdParamEmptySecondParamReturnsEmpty", fullNames[1].name)
        assertEquals("test_1_getOnSyntaxLanguagesWithQueryParams", noParamsNames[1].name)
        assertEquals("test_1_getOnSyntaxLanguagesWithQueryParamsFirstParamReturnsEmpty", onlyBooleanNames[1].name)
        assertEquals("test_1_getOnSyntaxLanguagesWithQueryParamsFirstParamNegativeThirdParamReturnsEmpty", booleanAndIntegerNames[1].name)
    }

    @Test
    fun sameTestRespectsMaxCharsLength() {
        val simpleIndividual = getEvaluatedIndividualWith(getRestCallAction("/languages"))
        val negativeQPIndividual = getEvaluatedIndividualWith(getRestCallAction("/languages", parameters = mutableListOf(getIntegerQueryParam("limit", false))))
        ensureGeneValue(negativeQPIndividual, "limit", "-1")

        val solution = Solution(mutableListOf(simpleIndividual, negativeQPIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val longTestCases = RestActionTestCaseNamingStrategy(solution, javaFormatter, QUERY_PARAMS_IN_NAME, 80).getTestCases()
        val shortTestCases = RestActionTestCaseNamingStrategy(solution, javaFormatter, QUERY_PARAMS_IN_NAME, 40).getTestCases()

        assertEquals(2, longTestCases.size)
        assertEquals(2, shortTestCases.size)
        assertEquals(shortTestCases[1].test, longTestCases[1].test)
        assertEquals("test_1_getOnLanguagesWithQueryParamNegativeLimitReturnsEmpty", longTestCases[1].name)
        assertEquals("test_1_getOnLanguagesWithQueryParam", shortTestCases[1].name)
    }


}
