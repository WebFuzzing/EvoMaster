package org.evomaster.core.output.naming

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getEvaluatedIndividualWith
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getRestCallAction
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.RestCallAction
import org.evomaster.core.problem.rest.RestIndividual
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.problem.rest.param.QueryParam
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.Solution
import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.optional.CustomMutationRateGene
import org.evomaster.core.search.gene.optional.OptionalGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Collections.singletonList

class TestCaseDisambiguationTest {

    companion object {
        val javaFormatter = LanguageConventionFormatter(OutputFormat.JAVA_JUNIT_4)
        const val NO_QUERY_PARAMS_IN_NAME = false
        const val QUERY_PARAMS_IN_NAME = true
    }

    @Test
    fun parentPathDisambiguation() {
        val funnyPathIndividual = getEvaluatedIndividualWith(getRestCallAction("/my/funny/path"))
        val funniestPathIndividual = getEvaluatedIndividualWith(getRestCallAction("/my/funniest/path"))

        val solution = Solution(mutableListOf(funnyPathIndividual, funniestPathIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, NO_QUERY_PARAMS_IN_NAME)

        val testCases = namingStrategy.getTestCases()
        assertEquals(2, testCases.size)
        assertEquals("test_0_getOnFunnyPathReturnsEmpty", testCases[0].name)
        assertEquals("test_1_getOnFunniestPathReturnsEmpty", testCases[1].name)
    }

    @Test
    fun pathsDifferAtRootDisambiguation() {
        val languagesIndividual = getEvaluatedIndividualWith(getRestCallAction("/languages"))
        val statisticsLanguagesIndividual = getEvaluatedIndividualWith(getRestCallAction("/statistics/languages"))
        val syntaxLanguagesIndividual = getEvaluatedIndividualWith(getRestCallAction("/syntax/languages"))


        val solution = Solution(mutableListOf(languagesIndividual, statisticsLanguagesIndividual, syntaxLanguagesIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, NO_QUERY_PARAMS_IN_NAME)

        val testCases = namingStrategy.getTestCases()
        assertEquals(3, testCases.size)
        assertEquals("test_0_getOnLanguagesReturnsEmpty", testCases[0].name)
        assertEquals("test_1_getOnStatisticsLanguagesReturnsEmpty", testCases[1].name)
        assertEquals("test_2_getOnSyntaxLanguagesReturnsEmpty", testCases[2].name)
    }

    @Test
    fun noDisambiguationWhenMoreThanOneIndividualSharePath() {
        val languagesIndividual = getEvaluatedIndividualWith(getRestCallAction("/languages"))
        val syntaxLanguagesIndividual = getEvaluatedIndividualWith(getRestCallAction("/syntax/languages"))
        val syntaxLanguagesIndividual2 = getEvaluatedIndividualWith(getRestCallAction("/syntax/languages"))


        val solution = Solution(mutableListOf(languagesIndividual, syntaxLanguagesIndividual, syntaxLanguagesIndividual2), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, NO_QUERY_PARAMS_IN_NAME)

        val testCases = namingStrategy.getTestCases()
        assertEquals(3, testCases.size)
        assertEquals("test_0_getOnLanguagesReturnsEmpty", testCases[0].name)
        assertEquals("test_1_getOnLanguagesReturnsEmpty", testCases[1].name)
        assertEquals("test_2_getOnLanguagesReturnsEmpty", testCases[2].name)
    }

    @Test
    fun pathWithUriParamsDisambiguation() {
        val configurationFeatureParameters = mutableListOf(getPathParam("productName"), getPathParam("configurationName"), getPathParam("featureName"))
        val configurationFeatureAction = getRestCallAction("/products/{productName}/configurations/{configurationName}/features/{featureName}", HttpVerb.GET, configurationFeatureParameters)
        val configurationFeatureIndividual = getEvaluatedIndividualWith(configurationFeatureAction)

        val productFeatureParameters = mutableListOf(getPathParam("productName"), getPathParam("featureName"))
        val productFeatureAction = getRestCallAction("/products/{productName}/features/{featureName}", HttpVerb.GET, productFeatureParameters)
        val productFeatureIndividual = getEvaluatedIndividualWith(productFeatureAction)

        val solution = Solution(mutableListOf(configurationFeatureIndividual, productFeatureIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, NO_QUERY_PARAMS_IN_NAME)

        val testCases = namingStrategy.getTestCases()
        assertEquals(2, testCases.size)
        assertEquals("test_0_getOnConfigurFeaturReturnsEmpty", testCases[0].name)
        assertEquals("test_1_getOnProductFeaturReturnsEmpty", testCases[1].name)
    }

    @Test
    fun pathWithQueryParamDisambiguation() {
        val syntaxLanguagesIndividual = getEvaluatedIndividualWith(getRestCallAction("/syntax/languages"))
        val syntaxLanguagesIndividualWithQP = getEvaluatedIndividualWith(getRestCallAction("/syntax/languages", parameters = singletonList(getStringQueryParam("myQueryParam"))))
        ensureGeneValue(syntaxLanguagesIndividualWithQP, "myQueryParam", "aStringValue")

        val solution = Solution(mutableListOf(syntaxLanguagesIndividual, syntaxLanguagesIndividualWithQP), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, NO_QUERY_PARAMS_IN_NAME)

        val testCases = namingStrategy.getTestCases()
        assertEquals(2, testCases.size)
        assertEquals("test_0_getOnSyntaxLanguagesReturnsEmpty", testCases[0].name)
        assertEquals("test_1_getOnSyntaxLanguagesWithQueryParamReturnsEmpty", testCases[1].name)
    }

    @Test
    fun pathWithMoreThanOneQueryParamDisambiguation() {
        val syntaxLanguagesIndividual = getEvaluatedIndividualWith(getRestCallAction("/syntax/languages"))
        val syntaxLanguagesIndividualWithQP = getEvaluatedIndividualWith(getRestCallAction("/syntax/languages", parameters = mutableListOf(getStringQueryParam("myQueryParam"), getStringQueryParam("myOtherQueryParam"))))
        ensureGeneValue(syntaxLanguagesIndividualWithQP, "myQueryParam", "aStringValue")
        ensureGeneValue(syntaxLanguagesIndividualWithQP, "myOtherQueryParam", "anotherStringValue")

        val solution = Solution(mutableListOf(syntaxLanguagesIndividual, syntaxLanguagesIndividualWithQP), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, NO_QUERY_PARAMS_IN_NAME)

        val testCases = namingStrategy.getTestCases()
        assertEquals(2, testCases.size)
        assertEquals("test_0_getOnSyntaxLanguagesReturnsEmpty", testCases[0].name)
        assertEquals("test_1_getOnSyntaxLanguagesWithQueryParamsReturnsEmpty", testCases[1].name)
    }

    @Test
    fun rootPathAndQueryParamDisambiguationReturnsThreeDifferentNames() {
        val languagesIndividual = getEvaluatedIndividualWith(getRestCallAction("/languages"))
        val syntaxLanguagesIndividual = getEvaluatedIndividualWith(getRestCallAction("/syntax/languages"))
        val syntaxLanguagesIndividualWithQP = getEvaluatedIndividualWith(getRestCallAction("/syntax/languages", parameters = singletonList(getStringQueryParam("myQueryParam"))))
        ensureGeneValue(syntaxLanguagesIndividualWithQP, "myQueryParam", "aStringValue")

        val solution = Solution(mutableListOf(languagesIndividual, syntaxLanguagesIndividual, syntaxLanguagesIndividualWithQP), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, NO_QUERY_PARAMS_IN_NAME)

        val testCases = namingStrategy.getTestCases()
        assertEquals(3, testCases.size)
        assertEquals("test_0_getOnLanguagesReturnsEmpty", testCases[0].name)
        assertEquals("test_1_getOnSyntaxLanguagesReturnsEmpty", testCases[1].name)
        assertEquals("test_2_getOnSyntaxLanguagesWithQueryParamReturnsEmpty", testCases[2].name)
    }

    @Test
    fun noQueryParamsAddedWhenNoQueryParamsInIndividual() {
        val syntaxLanguagesIndividual = getEvaluatedIndividualWith(getRestCallAction("/syntax/languages"))
        val syntaxLanguagesIndividual2 = getEvaluatedIndividualWith(getRestCallAction("/syntax/languages"))

        val solution = Solution(mutableListOf(syntaxLanguagesIndividual, syntaxLanguagesIndividual2), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, QUERY_PARAMS_IN_NAME)

        val testCases = namingStrategy.getTestCases()
        assertEquals(2, testCases.size)
        assertEquals("test_0_getOnLanguagesReturnsEmpty", testCases[0].name)
        assertEquals("test_1_getOnLanguagesReturnsEmpty", testCases[1].name)
    }

    @Test
    fun oneTrueBooleanQueryParamIsAdded() {
        val syntaxLanguagesIndividual = getEvaluatedIndividualWith(getRestCallAction("/syntax/languages"))
        val syntaxLanguagesIndividual2 = getEvaluatedIndividualWith(getRestCallAction("/syntax/languages", parameters = singletonList(getBooleanQueryParam("myQueryParam"))))
        ensureGeneValue(syntaxLanguagesIndividual2, "myQueryParam", "true")

        val solution = Solution(mutableListOf(syntaxLanguagesIndividual, syntaxLanguagesIndividual2), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, QUERY_PARAMS_IN_NAME)

        val testCases = namingStrategy.getTestCases()
        assertEquals(2, testCases.size)
        assertEquals("test_0_getOnSyntaxLanguagesReturnsEmpty", testCases[0].name)
        assertEquals("test_1_getOnSyntaxLanguagesWithQueryParamMyQueryParamReturnsEmpty", testCases[1].name)
    }

    @Test
    fun oneFalseBooleanQueryParamIsNotAdded() {
        val syntaxLanguagesIndividual = getEvaluatedIndividualWith(getRestCallAction("/syntax/languages"))
        val syntaxLanguagesIndividual2 = getEvaluatedIndividualWith(getRestCallAction("/syntax/languages", parameters = singletonList(getBooleanQueryParam("myQueryParam"))))
        ensureGeneValue(syntaxLanguagesIndividual2, "myQueryParam", "false")

        val solution = Solution(mutableListOf(syntaxLanguagesIndividual, syntaxLanguagesIndividual2), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, QUERY_PARAMS_IN_NAME)

        val testCases = namingStrategy.getTestCases()
        assertEquals(2, testCases.size)
        assertEquals("test_0_getOnSyntaxLanguagesReturnsEmpty", testCases[0].name)
        assertEquals("test_1_getOnSyntaxLanguagesWithQueryParamReturnsEmpty", testCases[1].name)
    }

    @Test
    fun onlyTrueBooleanQueryParamsAreAdded() {
        val syntaxLanguagesIndividual = getEvaluatedIndividualWith(getRestCallAction("/syntax/languages"))
        val syntaxLanguagesIndividual2 = getEvaluatedIndividualWith(getRestCallAction("/syntax/languages", parameters = mutableListOf(getBooleanQueryParam("firstParam"), getBooleanQueryParam("secondParam"), getStringQueryParam("thirdParam"), getBooleanQueryParam("fourthParam"))))
        ensureGeneValue(syntaxLanguagesIndividual2, "firstParam", "true")
        ensureGeneValue(syntaxLanguagesIndividual2, "secondParam", "false")
        ensureGeneValue(syntaxLanguagesIndividual2, "fourthParam", "true")

        val solution = Solution(mutableListOf(syntaxLanguagesIndividual, syntaxLanguagesIndividual2), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, QUERY_PARAMS_IN_NAME)

        val testCases = namingStrategy.getTestCases()
        assertEquals(2, testCases.size)
        assertEquals("test_0_getOnSyntaxLanguagesReturnsEmpty", testCases[0].name)
        assertEquals("test_1_getOnSyntaxLanguagesWithQueryParamsFirstParamAndFourthParamReturnsEmpty", testCases[1].name)
    }

    @Test
    fun negativeNumberQueryParamIsAdded() {
        val simpleIndividual = getEvaluatedIndividualWith(getRestCallAction("/languages"))
        val negativeQPIndividual = getEvaluatedIndividualWith(getRestCallAction("/languages", parameters = mutableListOf(getIntegerQueryParam("limit"))))
        ensureGeneValue(negativeQPIndividual, "limit", "-1")

        val solution = Solution(mutableListOf(simpleIndividual, negativeQPIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, QUERY_PARAMS_IN_NAME)

        val testCases = namingStrategy.getTestCases()
        assertEquals(2, testCases.size)
        assertEquals("test_0_getOnLanguagesReturnsEmpty", testCases[0].name)
        assertEquals("test_1_getOnLanguagesWithQueryParamNegativeLimitReturnsEmpty", testCases[1].name)
    }

    @Test
    fun emptyStringQueryParamIsAdded() {
        val simpleIndividual = getEvaluatedIndividualWith(getRestCallAction("/languages"))
        val emptyStringIndividual = getEvaluatedIndividualWith(getRestCallAction("/languages", parameters = mutableListOf(getStringQueryParam("name"))))
        ensureGeneValue(emptyStringIndividual, "name", "")

        val solution = Solution(mutableListOf(simpleIndividual, emptyStringIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, QUERY_PARAMS_IN_NAME)

        val testCases = namingStrategy.getTestCases()
        assertEquals(2, testCases.size)
        assertEquals("test_0_getOnLanguagesReturnsEmpty", testCases[0].name)
        assertEquals("test_1_getOnLanguagesWithQueryParamEmptyNameReturnsEmpty", testCases[1].name)
    }

    @Test
    fun emptyStringAndNegativeIntQueryParamsAreAdded() {
        val simpleIndividual = getEvaluatedIndividualWith(getRestCallAction("/languages"))
        val queryParamsIndividual = getEvaluatedIndividualWith(getRestCallAction("/languages", parameters = mutableListOf(getStringQueryParam("name"), getIntegerQueryParam("limit"))))
        ensureGeneValue(queryParamsIndividual, "name", "")
        ensureGeneValue(queryParamsIndividual, "limit", "-1")

        val solution = Solution(mutableListOf(simpleIndividual, queryParamsIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter, QUERY_PARAMS_IN_NAME)

        val testCases = namingStrategy.getTestCases()
        assertEquals(2, testCases.size)
        assertEquals("test_0_getOnLanguagesReturnsEmpty", testCases[0].name)
        assertEquals("test_1_getOnLanguagesWithQueryParamsNegativeLimitEmptyNameReturnsEmpty", testCases[1].name)
    }

    private fun getPathParam(paramName: String): Param {
        return PathParam(paramName, CustomMutationRateGene(paramName, StringGene(paramName), 1.0))
    }

    private fun getStringQueryParam(paramName: String): Param {
        return getQueryParam(paramName, StringGene(paramName))
    }

    private fun getBooleanQueryParam(paramName: String): Param {
        return getQueryParam(paramName, BooleanGene(paramName))
    }

    private fun getIntegerQueryParam(paramName: String, wrapped: Boolean = true): Param {
        return getQueryParam(paramName, IntegerGene(paramName), wrapped)
    }

    private fun getQueryParam(paramName: String, gene: Gene, wrapped: Boolean = true): Param {
        return QueryParam(paramName, if (wrapped) getWrappedGene(paramName, gene) else gene)
    }

    private fun getWrappedGene(paramName: String, gene: Gene): OptionalGene {
        return OptionalGene(paramName, gene)
    }

    /*
        Since the randomization used to construct the evaluated individuals might set a random boolean value,
        we do this to ensure the one we want for unit testing
     */
    private fun ensureGeneValue(evaluatedIndividual: EvaluatedIndividual<RestIndividual>, paramName: String, paramValue: String) {
        val restCallAction = evaluatedIndividual.evaluatedMainActions().last().action as RestCallAction
        (restCallAction.parameters.filter { it.name == paramName }).forEach {
            (it as QueryParam).getGeneForQuery().setFromStringValue(paramValue)
        }
    }

}
