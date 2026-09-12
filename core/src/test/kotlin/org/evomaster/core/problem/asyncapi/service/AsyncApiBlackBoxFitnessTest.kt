package org.evomaster.core.problem.asyncapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.AbstractModule
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.google.inject.util.Modules
import com.netflix.governator.guice.LifecycleInjector
import com.webfuzzing.asyncapi.access.AsyncApiAccess
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.problem.AsyncApiProblemDto
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto
import org.evomaster.core.BaseModule
import org.evomaster.core.problem.asyncapi.data.AsyncApiAction
import org.evomaster.core.problem.asyncapi.data.AsyncApiCallResult
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual
import org.evomaster.core.problem.asyncapi.data.AsyncApiOutcome
import org.evomaster.core.problem.asyncapi.service.FakeAsyncApiDriver.Companion.fireAndForget
import org.evomaster.core.problem.asyncapi.service.FakeAsyncApiDriver.Companion.replied
import org.evomaster.core.problem.asyncapi.service.FakeAsyncApiDriver.Companion.silence
import org.evomaster.core.problem.enterprise.ExperimentalFaultCategory
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.remote.service.RemoteController
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.IdMapper
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchGlobalState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AsyncApiBlackBoxFitnessTest {

    companion object {
        private const val NCS = "/asyncapi/sut/ncs-kafka.yaml"

        private const val DOUBLE_RESULT = """{"resultAsDouble": 1.5}"""

        private const val ERROR = """{"error": {"code": 400, "message": "n must be >= 3"}}"""

        /**
         * One operation that expects nothing back, and one whose message carries a header
         * besides the one the correlation id is stamped into.
         */
        private val EVENTS = """
            asyncapi: 3.0.0
            info:
              title: Events
              version: 1.0.0
            channels:
              events:
                address: app.events
                messages:
                  event:
                    headers:
                      type: object
                      required: [tenant]
                      properties:
                        tenant:
                          type: string
                        correlationId:
                          type: string
                    correlationId:
                      location: '${'$'}message.header#/correlationId'
                    payload:
                      type: object
                      required: [id]
                      properties:
                        id:
                          type: string
            operations:
              publishEvent:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/events'
        """.trimIndent()
    }

    private lateinit var injector: Injector
    private lateinit var driver: FakeAsyncApiDriver
    private lateinit var sampler: AsyncApiSampler
    private lateinit var fitness: FitnessFunction<AsyncApiIndividual>
    private lateinit var idMapper: IdMapper

    @BeforeEach
    fun reset() {
        RestActionBuilderV3.cleanCache()
    }

    private fun start(schemaText: String, answer: (AsyncApiActionDto) -> AsyncApiReplyDto?) {

        val info = SutInfoDto().apply {
            asyncApiProblem = AsyncApiProblemDto().apply { this.schemaText = schemaText }
            defaultOutputFormat = SutInfoDto.OutputFormat.KOTLIN_JUNIT_5
        }
        driver = FakeAsyncApiDriver(info, answer)

        val args = arrayOf("--seed=42", "--problemType=ASYNCAPI", "--blackBox=false", "--createTests=false")

        val fake = object : AbstractModule() {
            override fun configure() {
                bind(RemoteController::class.java).toInstance(driver)
            }
        }

        injector = LifecycleInjector.builder()
            .withModules(listOf(BaseModule(args), Modules.override(AsyncApiModule()).with(fake)))
            .build().createInjector()

        sampler = injector.getInstance(AsyncApiSampler::class.java)
        fitness = injector.getInstance(Key.get(object : TypeLiteral<FitnessFunction<AsyncApiIndividual>>() {}))
        idMapper = injector.getInstance(IdMapper::class.java)
    }

    private fun startNcs(answer: (AsyncApiActionDto) -> AsyncApiReplyDto?) =
        start(AsyncApiAccess.readFromResource(NCS), answer)

    private fun individualOf(vararg names: String): AsyncApiIndividual {

        val randomness = injector.getInstance(Randomness::class.java)

        val actions = names.map { name ->
            val template = sampler.seeAvailableActions().first { it.getName() == name }
            (template.copy() as AsyncApiAction).apply { doInitialize(randomness) }
        }

        return AsyncApiIndividual(SampleType.RANDOM, actions.toMutableList()).apply {
            doGlobalInitialize(injector.getInstance(SearchGlobalState::class.java))
        }
    }

    private fun evaluate(vararg names: String): EvaluatedIndividual<AsyncApiIndividual> =
        fitness.calculateCoverage(individualOf(*names), modifiedSpec = null)
            ?: fail("the fitness gave up on the individual")

    private fun coveredIds(evaluated: EvaluatedIndividual<AsyncApiIndividual>): Set<String> =
        evaluated.fitness.coveredTargets().map { idMapper.getDescriptiveId(it) }.toSet()

    private fun results(evaluated: EvaluatedIndividual<AsyncApiIndividual>): List<AsyncApiCallResult> =
        evaluated.evaluatedMainActions().map { it.result as AsyncApiCallResult }

    @Test
    fun testTheDriverIsToldEverythingItNeeds() {

        startNcs { replied(DOUBLE_RESULT) }

        evaluate("bessj")

        val dto = driver.published.single()

        assertEquals("bessj", dto.operationId)
        assertEquals("bessjRequest", dto.channelName)
        assertEquals("bessjRequest", dto.messageId)
        //resolved from the document, so the driver never has to read it
        assertEquals("ncs.bessj.request", dto.address)
        assertEquals("application/json", dto.contentType)
        assertEquals("ncs.bessj.reply", dto.replyAddress)
        assertEquals(5000L, dto.replyTimeoutMs)

        val payload = ObjectMapper().readTree(dto.payload)
        assertTrue(payload.isObject, "payload is not a JSON object: ${dto.payload}")
        assertEquals(setOf("n", "x"), payload.fieldNames().asSequence().toSet())

        assertEquals(AsyncApiActionDto.CORRELATION_IN_HEADER, dto.correlationLocation)
        assertEquals("/correlationId", dto.correlationPointer)
        assertFalse(dto.correlationId.isNullOrBlank())
        assertTrue(dto.headers.isEmpty())
    }

    @Test
    fun testEachPublishedMessageGetsItsOwnCorrelationId() {

        startNcs { replied(DOUBLE_RESULT) }

        evaluate("bessj", "expint", "gammq")
        evaluate("bessj", "expint", "gammq")

        val ids = driver.published.map { it.correlationId }
        assertEquals(6, ids.size)
        assertEquals(6, ids.toSet().size, "correlation ids repeat: $ids")
    }

    @Test
    fun testARecognisedReplyCoversTheOperationAndTheDeclaredMessage() {

        startNcs { replied(DOUBLE_RESULT) }

        val evaluated = evaluate("bessj")
        val covered = coveredIds(evaluated)

        assertTrue(covered.contains("ASYNCAPI_OUTCOME:REPLIED:bessj"), "$covered")
        assertTrue(covered.contains("ASYNCAPI_REPLY:doubleResult:bessj"), "$covered")
        assertTrue(covered.none { IdMapper.isFault(it) }, "$covered")

        val result = results(evaluated).single()
        assertEquals(AsyncApiOutcome.REPLIED, result.getOutcome())
        assertEquals("doubleResult", result.getReplyMessage())
        assertEquals(DOUBLE_RESULT, result.getReplyPayload())
        assertEquals(true, result.getCorrelationMatched())
        assertFalse(result.stopping)
    }

    @Test
    fun testTheErrorReplyIsADifferentTargetFromTheResult() {

        startNcs { replied(ERROR) }

        val covered = coveredIds(evaluate("bessj"))

        assertTrue(covered.contains("ASYNCAPI_REPLY:error:bessj"), "$covered")
        assertFalse(covered.contains("ASYNCAPI_REPLY:doubleResult:bessj"))
        //an error reply the contract declares is the service behaving, not a fault
        assertTrue(covered.none { IdMapper.isFault(it) }, "$covered")
    }

    @Test
    fun testAReplyTheContractDoesNotDeclareIsAFault() {

        startNcs { replied("""{"something": "else"}""") }

        val evaluated = evaluate("bessj")
        val faults = evaluated.fitness.coveredTargets().filter { idMapper.isFault(it) }

        assertEquals(1, faults.size, coveredIds(evaluated).toString())
        assertTrue(idMapper.isSpecifiedFault(faults.single(), ExperimentalFaultCategory.ASYNCAPI_UNDECLARED_REPLY))
        assertNull(results(evaluated).single().getReplyMessage())
    }

    @Test
    fun testSilenceAfterAPromisedReplyIsAFault() {

        startNcs { silence(waited = 5000) }

        val evaluated = evaluate("bessj")
        val covered = coveredIds(evaluated)
        val faults = evaluated.fitness.coveredTargets().filter { idMapper.isFault(it) }

        assertTrue(covered.contains("ASYNCAPI_OUTCOME:NO_REPLY:bessj"), "$covered")
        assertEquals(1, faults.size, "$covered")
        assertTrue(idMapper.isSpecifiedFault(faults.single(), ExperimentalFaultCategory.ASYNCAPI_NO_REPLY))

        val result = results(evaluated).single()
        assertEquals(AsyncApiOutcome.NO_REPLY, result.getOutcome())
        assertEquals(5000L, result.getWaitedMs())
        //silence is a finding, not a broken setup: the test goes on
        assertFalse(result.stopping)
    }

    @Test
    fun testAFireAndForgetOperationIsCoveredByBeingPublished() {

        start(EVENTS) { fireAndForget() }

        val evaluated = evaluate("publishEvent")
        val covered = coveredIds(evaluated)

        assertTrue(covered.contains("ASYNCAPI_OUTCOME:PUBLISHED:publishEvent"), "$covered")
        assertTrue(covered.none { IdMapper.isFault(it) }, "$covered")
        assertEquals(AsyncApiOutcome.PUBLISHED, results(evaluated).single().getOutcome())

        //no reply declared, so nothing to wait for
        val dto = driver.published.single()
        assertNull(dto.replyAddress)
        assertNull(dto.replyTimeoutMs)
    }

    @Test
    fun testHeadersTravelAsAMapWithoutTheStampedOne() {

        start(EVENTS) { fireAndForget() }

        evaluate("publishEvent")

        val dto = driver.published.single()

        //the tenant header is the search's to vary; the correlation id is the driver's to stamp
        assertEquals(setOf("tenant"), dto.headers.keys)
        assertEquals(AsyncApiActionDto.CORRELATION_IN_HEADER, dto.correlationLocation)
        assertEquals("/correlationId", dto.correlationPointer)
    }

    @Test
    fun testAMessageTheDriverCannotPublishStopsTheTest() {

        //a driver that cannot be reached: the remote controller reports that as no reply at all
        startNcs { null }

        val evaluated = evaluate("bessj", "expint", "gammq")

        assertEquals(1, driver.published.size, "the test went on after a message that never left")

        val first = results(evaluated).first()
        assertEquals(AsyncApiOutcome.PUBLISH_FAILED, first.getOutcome())
        assertTrue(first.stopping)
        assertNotNull(first.getErrorMessage())

        //a broken setup is not a finding about the service, so nothing is covered by it
        assertTrue(coveredIds(evaluated).none { it.startsWith("ASYNCAPI") }, coveredIds(evaluated).toString())
    }
}
