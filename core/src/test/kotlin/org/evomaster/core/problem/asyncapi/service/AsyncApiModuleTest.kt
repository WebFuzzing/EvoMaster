package org.evomaster.core.problem.asyncapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.webfuzzing.asyncapi.access.AsyncApiAccess
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual
import org.evomaster.core.problem.asyncapi.service.FakeAsyncApiDriver.Companion.replied
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.search.algorithms.MioAlgorithm
import org.evomaster.core.search.service.IdMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * A whole search, through the module Main binds, against a driver standing in for the NCS
 * service over Kafka: the first time `--problemType ASYNCAPI` does everything but write tests.
 */
class AsyncApiModuleTest {

    companion object {
        private const val NCS = "/asyncapi/sut/ncs-kafka.yaml"

        private val NCS_OPERATIONS = setOf("checkTriangle", "bessj", "expint", "fisher", "gammq", "remainder")

        private val mapper = ObjectMapper()
    }

    @BeforeEach
    fun reset() {
        RestActionBuilderV3.cleanCache()
    }

    /**
     * Replies as the NCS service would, judging by the request alone: each operation has inputs
     * it rejects, and answers those with the declared error message instead of a result.
     */
    private fun ncsLike(call: AsyncApiActionDto): AsyncApiReplyDto {

        val request = try {
            mapper.readTree(call.payload)
        } catch (e: Exception) {
            return replied(error("not JSON"))
        }

        fun number(name: String) = request.get(name)?.takeIf { it.isNumber }?.asDouble()

        val a = number("a")
        val b = number("b")
        val c = number("c")
        val n = number("n")
        val m = number("m")
        val x = number("x")

        val ok = when (call.operationId) {
            "checkTriangle" -> a != null && b != null && c != null && a > 0 && b > 0 && c > 0
            "bessj" -> n != null && x != null && n >= 3 && n <= 1000
            "expint" -> n != null && x != null && n >= 0 && x >= 0
            "fisher" -> m != null && n != null && x != null && m in 1.0..1000.0 && n in 1.0..1000.0
            "gammq" -> a != null && x != null && a > 0 && x >= 0
            "remainder" -> a != null && b != null && b != 0.0
            else -> false
        }

        if (!ok) {
            return replied(error("rejected"))
        }

        val integerResult = call.operationId == "checkTriangle" || call.operationId == "remainder"

        return replied(if (integerResult) """{"resultAsInt": 1}""" else """{"resultAsDouble": 0.5}""")
    }

    private fun error(message: String) = """{"error": {"code": 400, "message": "$message"}}"""

    @Test
    fun testASearchReachesBothDeclaredRepliesOfAnOperation() {

        val driver = FakeAsyncApiDriver(AsyncApiTestInjector.sutInfo(AsyncApiAccess.readFromResource(NCS))) { ncsLike(it) }

        //MIO is asked for explicitly, since it is what is instantiated below and tracking follows the option
        val injector = AsyncApiTestInjector.create(
            driver,
            "--blackBox=true",
            "--algorithm=MIO",
            "--stoppingCriterion=ACTION_EVALUATIONS",
            "--maxEvaluations=100",
            "--maxTestSize=3",
            "--useTimeInFeedbackSampling=false"
        )

        val mio = injector.getInstance(Key.get(object : TypeLiteral<MioAlgorithm<AsyncApiIndividual>>() {}))
        val solution = mio.search()

        val idMapper = injector.getInstance(IdMapper::class.java)
        val covered = solution.overall.coveredTargets().map { idMapper.getDescriptiveId(it) }.toSet()

        /*
            Every operation rejects some inputs, and a hundred evaluations are plenty for the
            search to meet both the result and the error of at least one of them. That the two
            are distinct targets is the point of the whole exercise; which operations get there
            first depends on the seed, so no particular one is asked for.
         */
        val bothRepliesReached = NCS_OPERATIONS.filter { op ->
            covered.contains("ASYNCAPI_REPLY:error:$op")
                    && (covered.contains("ASYNCAPI_REPLY:doubleResult:$op") || covered.contains("ASYNCAPI_REPLY:intResult:$op"))
        }
        assertTrue(bothRepliesReached.isNotEmpty(), "no operation reached both of its replies: $covered")

        //every operation was published to and answered
        NCS_OPERATIONS.forEach { assertTrue(covered.contains("ASYNCAPI_OUTCOME:REPLIED:$it"), "$it missing in $covered") }

        //the stand-in always answers with a declared message, so nothing here is a fault
        assertTrue(covered.none { IdMapper.isFault(it) }, "$covered")

        assertTrue(solution.individuals.isNotEmpty())
        assertTrue(driver.published.size >= 50, "only ${driver.published.size} messages published")
        assertEquals(driver.published.size, driver.published.map { it.correlationId }.toSet().size)
    }
}
