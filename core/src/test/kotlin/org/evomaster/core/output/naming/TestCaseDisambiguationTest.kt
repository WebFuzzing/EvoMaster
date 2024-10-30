package org.evomaster.core.output.naming

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getEvaluatedIndividualWith
import org.evomaster.core.output.naming.RestActionTestCaseUtils.getRestCallAction
import org.evomaster.core.problem.api.param.Param
import org.evomaster.core.problem.rest.HttpVerb
import org.evomaster.core.problem.rest.param.PathParam
import org.evomaster.core.search.Solution
import org.evomaster.core.search.gene.optional.CustomMutationRateGene
import org.evomaster.core.search.gene.string.StringGene
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TestCaseDisambiguationTest {

    companion object {
        val javaFormatter = LanguageConventionFormatter(OutputFormat.JAVA_JUNIT_4)
    }

    @Test
    fun parentPathDisambiguation() {
        val funnyPathIndividual = getEvaluatedIndividualWith(getRestCallAction("/my/funny/path"))
        val funniestPathIndividual = getEvaluatedIndividualWith(getRestCallAction("/my/funniest/path"))

        val solution = Solution(mutableListOf(funnyPathIndividual, funniestPathIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(2, testCases.size)
        assertEquals("test_0_getOnFunnyPathReturnsEmpty", testCases[0].name)
        assertEquals("test_1_getOnFunniestPathReturnsEmpty", testCases[1].name)
    }

    @Test
    fun pathsDifferAtRootDisambiguation() {
        val languagesIndividual = getEvaluatedIndividualWith(getRestCallAction("/languages"))
        val statisticsLanguagesIndividual = getEvaluatedIndividualWith(getRestCallAction("/statistics/languages"))


        val solution = Solution(mutableListOf(languagesIndividual, statisticsLanguagesIndividual), "suitePrefix", "suiteSuffix", Termination.NONE, emptyList(), emptyList())

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(2, testCases.size)
        assertEquals("test_0_getOnLanguagesReturnsEmpty", testCases[0].name)
        assertEquals("test_1_getOnStatisticsLanguagesReturnsEmpty", testCases[1].name)
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

        val namingStrategy = RestActionTestCaseNamingStrategy(solution, javaFormatter)

        val testCases = namingStrategy.getTestCases()
        assertEquals(2, testCases.size)
        assertEquals("test_0_getOnConfigurFeaturReturnsEmpty", testCases[0].name)
        assertEquals("test_1_getOnProductFeaturReturnsEmpty", testCases[1].name)
    }

    private fun getPathParam(paramName: String): Param {
        return PathParam(paramName, CustomMutationRateGene(paramName, StringGene(paramName), 1.0))
    }

}
