package org.evomaster.core.output.sorting

import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.webfuzzing.asyncapi.access.AsyncApiAccess
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto
import org.evomaster.core.problem.asyncapi.data.AsyncApiAction
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual
import org.evomaster.core.problem.asyncapi.service.AsyncApiSampler
import org.evomaster.core.problem.asyncapi.service.AsyncApiTestInjector
import org.evomaster.core.problem.asyncapi.service.FakeAsyncApiDriver
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchGlobalState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * The order the tests of an AsyncAPI suite end up in.
 *
 * Sorting happens only when a suite is written, which is why no other test reaches this: the
 * comparator used to throw on an AsyncAPI individual, and nothing noticed until a whole run did.
 */
class AsyncApiSortingTest {

    companion object {
        private const val DOUBLE_RESULT = """{"resultAsDouble": 1.5}"""

        private const val ERROR = """{"code": 400, "message": "no"}"""
    }

    @BeforeEach
    fun reset() {
        RestActionBuilderV3.cleanCache()
    }

    @Test
    fun testTestsAreGroupedByOperationThenByWhatPublishingDid() {

        val silent = evaluate("expint") { FakeAsyncApiDriver.silence() }
        val replied = evaluate("bessj") { FakeAsyncApiDriver.replied(DOUBLE_RESULT) }
        val error = evaluate("expint") { FakeAsyncApiDriver.replied(ERROR) }

        val tests = mutableListOf(silent, error, replied)

        SortingHelper().sort(tests, SortingStrategy.TARGET_INCREMENTAL)

        /*
            bessj before expint, as the operation groups first; then within expint the reply that
            arrived before the silence, as REPLIED comes before NO_REPLY.
         */
        assertEquals(listOf("bessj", "expint", "expint"), tests.map { operationOf(it) })
        assertEquals(replied, tests[0])
        assertEquals(error, tests[1])
        assertEquals(silent, tests[2])
    }

    @Test
    fun testSortingASingleTestDoesNotThrow() {

        val tests = mutableListOf(evaluate("bessj") { FakeAsyncApiDriver.replied(DOUBLE_RESULT) })

        SortingHelper().sort(tests, SortingStrategy.TARGET_INCREMENTAL)

        assertEquals(1, tests.size)
    }

    private fun operationOf(evaluated: EvaluatedIndividual<*>) =
        (evaluated.evaluatedMainActions().last().action as AsyncApiAction).operationId

    private fun evaluate(
        operation: String,
        answer: (AsyncApiActionDto) -> AsyncApiReplyDto?
    ): EvaluatedIndividual<AsyncApiIndividual> {

        val driver = FakeAsyncApiDriver(AsyncApiTestInjector.sutInfo(AsyncApiAccess.readFromResource(AsyncApiTestInjector.NCS)), answer)
        val injector = AsyncApiTestInjector.create(driver, "--blackBox=false")

        val sampler = injector.getInstance(AsyncApiSampler::class.java)
        val fitness = injector.getInstance(Key.get(object : TypeLiteral<FitnessFunction<AsyncApiIndividual>>() {}))
        val randomness = injector.getInstance(Randomness::class.java)

        val template = sampler.seeAvailableActions().first { it.getName() == operation }
        val action = (template.copy() as AsyncApiAction).apply { doInitialize(randomness) }

        val individual = AsyncApiIndividual(SampleType.RANDOM, mutableListOf(action)).apply {
            doGlobalInitialize(injector.getInstance(SearchGlobalState::class.java))
        }

        return fitness.calculateCoverage(individual, modifiedSpec = null)
            ?: throw IllegalStateException("the fitness gave up on the individual")
    }
}
