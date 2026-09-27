package org.evomaster.core.output.naming

import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.webfuzzing.asyncapi.access.AsyncApiAccess
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.problem.asyncapi.data.AsyncApiAction
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual
import org.evomaster.core.problem.asyncapi.service.AsyncApiSampler
import org.evomaster.core.problem.asyncapi.service.AsyncApiTestInjector
import org.evomaster.core.problem.asyncapi.service.FakeAsyncApiDriver
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.search.Solution
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchGlobalState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * What a test over an AsyncAPI service is called. There is no status code to name an outcome
 * with, so the name carries the outcome itself, and the declared message a reply was recognised
 * as where there is one.
 */
class AsyncApiActionNamingStrategyTest {

    companion object {
        private const val NCS = "/asyncapi/sut/ncs-kafka.yaml"

        private const val DOUBLE_RESULT = """{"resultAsDouble": 1.5}"""

        private val outputFormat = OutputFormat.KOTLIN_JUNIT_5

        private val formatter = LanguageConventionFormatter(outputFormat)

        private const val MAX_NAME_LENGTH = 80
    }

    @BeforeEach
    fun reset() {
        RestActionBuilderV3.cleanCache()
    }

    @Test
    fun testAReplyIsNamedAfterTheDeclaredMessageItWasRecognisedAs() {

        val names = namesOf({ FakeAsyncApiDriver.replied(DOUBLE_RESULT) }, "bessj")

        assertEquals(listOf("test_0_publishOnBessjReturnsDoubleResult"), names)
    }

    @Test
    fun testAnOperationThatExpectsNothingBackSaysSo() {

        val names = namesOf({ FakeAsyncApiDriver.fireAndForget() }, "bessj")

        assertEquals(listOf("test_0_publishOnBessjReturnsNothing"), names)
    }

    @Test
    fun testSilenceIsNamedTheSameWhetherOrNotItIsReportedAsAFault() {

        /*
            With the experimental oracles off there is no fault, so the outcome names the test;
            with them on the fault label does. Both have to read the same, or the name of a test
            would change with a flag that is about reporting.
         */
        val withoutOracles = namesOf({ FakeAsyncApiDriver.silence() }, "bessj")
        val withOracles = namesOf({ FakeAsyncApiDriver.silence() }, "bessj", "--useExperimentalOracles=true")

        assertEquals(listOf("test_0_publishOnBessjGetsNoReply"), withoutOracles)
        assertEquals(withoutOracles, withOracles)
    }

    @Test
    fun testTheOperationIsKeptWhenTheOutcomeNoLongerFits() {

        //tokens are dropped whole, left to right, so the operation outlives the outcome
        val solution = solutionOf({ FakeAsyncApiDriver.replied(DOUBLE_RESULT) }, "bessj")

        val roomForBoth = AsyncApiActionTestCaseNamingStrategy(solution, formatter, MAX_NAME_LENGTH).getTestCases()
        val roomForTheOperation = AsyncApiActionTestCaseNamingStrategy(solution, formatter, 25).getTestCases()
        val roomForNeither = AsyncApiActionTestCaseNamingStrategy(solution, formatter, 10).getTestCases()

        assertEquals("test_0_publishOnBessjReturnsDoubleResult", roomForBoth[0].name)
        assertEquals("test_0_publishOnBessj", roomForTheOperation[0].name)
        assertEquals("test_0", roomForNeither[0].name)
    }

    private fun namesOf(
        answer: (AsyncApiActionDto) -> AsyncApiReplyDto?,
        operation: String,
        vararg options: String
    ): List<String> {
        val solution = solutionOf(answer, operation, options = options)
        return AsyncApiActionTestCaseNamingStrategy(solution, formatter, MAX_NAME_LENGTH)
            .getTestCases()
            .map { it.name }
    }

    private fun solutionOf(
        answer: (AsyncApiActionDto) -> AsyncApiReplyDto?,
        operation: String,
        options: Array<out String> = arrayOf()
    ): Solution<AsyncApiIndividual> {

        val driver = FakeAsyncApiDriver(AsyncApiTestInjector.sutInfo(AsyncApiAccess.readFromResource(NCS)), answer)
        val injector = AsyncApiTestInjector.create(driver, "--blackBox=false", *options)

        val sampler = injector.getInstance(AsyncApiSampler::class.java)
        val fitness = injector.getInstance(Key.get(object : TypeLiteral<FitnessFunction<AsyncApiIndividual>>() {}))
        val randomness = injector.getInstance(Randomness::class.java)

        val template = sampler.seeAvailableActions().first { it.getName() == operation }
        val actions = listOf((template.copy() as AsyncApiAction).apply { doInitialize(randomness) })

        val individual = AsyncApiIndividual(SampleType.RANDOM, actions.toMutableList()).apply {
            doGlobalInitialize(injector.getInstance(SearchGlobalState::class.java))
        }

        val evaluated = fitness.calculateCoverage(individual, modifiedSpec = null)
            ?: throw IllegalStateException("the fitness gave up on the individual")

        return Solution(
            mutableListOf(evaluated),
            "suitePrefix",
            "suiteSuffix",
            Termination.NONE,
            listOf(),
            listOf()
        )
    }
}
