package org.evomaster.core.output.sorting

import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto
import org.evomaster.core.problem.asyncapi.data.AsyncApiAction
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual
import org.evomaster.core.problem.asyncapi.service.AsyncApiTestInjector
import org.evomaster.core.problem.asyncapi.service.AsyncApiTestInjector.DOUBLE_RESULT
import org.evomaster.core.problem.asyncapi.service.FakeAsyncApiDriver
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.search.EvaluatedIndividual
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * The order the tests of an AsyncAPI suite end up in.
 *
 * Sorting happens only when a suite is written, which is why no other test reaches this: the
 * comparator used to throw on an AsyncAPI individual, and nothing noticed until a whole run did.
 */
class SortingHelperTest {

    companion object {
        //as the document declares it: the fields sit under "error", not at the top level
        private const val ERROR = """{"error": {"code": 400, "message": "no"}}"""
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
    fun testRepliesToOneOperationAreOrderedByTheMessageTheyWereRecognisedAs() {

        /*
            The third key, which the operation and the outcome cannot separate: two tests of one
            operation that both got a reply, telling them apart by which declared message it
            was. A comparator of two keys leaves these in whatever order they arrived.
         */
        val error = evaluate("expint") { FakeAsyncApiDriver.replied(ERROR) }
        val result = evaluate("expint") { FakeAsyncApiDriver.replied(DOUBLE_RESULT) }

        val tests = mutableListOf(result, error)

        SortingHelper().sort(tests, SortingStrategy.TARGET_INCREMENTAL)

        //"doubleResult" before "error", as the comparator orders them by the message's own name
        assertEquals(result, tests[0])
        assertEquals(error, tests[1])
    }

    private fun operationOf(evaluated: EvaluatedIndividual<*>) =
        (evaluated.evaluatedMainActions().last().action as AsyncApiAction).operationId

    private fun evaluate(
        operation: String,
        answer: (AsyncApiActionDto) -> AsyncApiReplyDto?
    ): EvaluatedIndividual<AsyncApiIndividual> {

        val injector = AsyncApiTestInjector.create(AsyncApiTestInjector.ncsDriver(answer), "--blackBox=false")

        return AsyncApiTestInjector.evaluate(injector, operation)
    }
}
