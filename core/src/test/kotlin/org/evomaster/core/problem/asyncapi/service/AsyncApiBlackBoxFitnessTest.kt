package org.evomaster.core.problem.asyncapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.webfuzzing.asyncapi.access.AsyncApiAccess
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto
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
                      required: [tenant, meta]
                      properties:
                        tenant:
                          type: string
                        meta:
                          type: object
                          required: [v]
                          properties:
                            v:
                              type: integer
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

        /**
         * A request whose reply comes back wherever the request says, rather than on a channel
         * the contract fixes.
         */
        private val DYNAMIC_REPLY = """
            asyncapi: 3.0.0
            info:
              title: Dynamic reply
              version: 1.0.0
            channels:
              requests:
                address: app.requests
                messages:
                  request:
                    headers:
                      type: object
                      properties:
                        replyTo:
                          type: string
                    payload:
                      type: object
                      required: [id]
                      properties:
                        id:
                          type: string
            operations:
              ask:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/requests'
                reply:
                  address:
                    location: '${'$'}message.header#/replyTo'
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

        driver = FakeAsyncApiDriver(AsyncApiTestInjector.sutInfo(schemaText), answer)
        injector = AsyncApiTestInjector.create(driver, "--blackBox=false")

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

        //the declared headers are the search's to vary; the correlation id is the driver's to stamp
        assertEquals(setOf("tenant", "meta"), dto.headers.keys)
        assertEquals(AsyncApiActionDto.CORRELATION_IN_HEADER, dto.correlationLocation)
        assertEquals("/correlationId", dto.correlationPointer)

        //a header that is itself structured travels as its JSON
        val meta = ObjectMapper().readTree(dto.headers.getValue("meta"))
        assertTrue(meta.isObject && meta.has("v"), "meta header: ${dto.headers["meta"]}")
    }

    @Test
    fun testTheCorrelationIdMayBelongInThePayload() {

        start(AsyncApiAccess.readFromResource("/asyncapi/artificial/websocket-reply.yaml")) {
            replied("""{"request_id": "r", "legs": []}""")
        }

        evaluate("recv_list_legs")

        val dto = driver.published.single()

        //MQTT 3.1.1 and raw WebSocket have no metadata, so such documents carry the id inside the message
        assertEquals(AsyncApiActionDto.CORRELATION_IN_PAYLOAD, dto.correlationLocation)
        assertEquals("/request_id", dto.correlationPointer)
        assertEquals("/v1/vsi", dto.address)
        assertEquals("/v1/vsi", dto.replyAddress)
    }

    @Test
    fun testAReplyAddressAnnouncedAtRunTimeIsNotWaitedForYet() {

        start(DYNAMIC_REPLY) { fireAndForget() }

        val evaluated = evaluate("ask")

        //the contract does promise a reply, but there is nowhere fixed to wait for it
        assertTrue((sampler.seeAvailableActions().single() as AsyncApiAction).expectsReply())
        val dto = driver.published.single()
        assertNull(dto.replyAddress)
        assertNull(dto.replyTimeoutMs)

        assertEquals(AsyncApiOutcome.PUBLISHED, results(evaluated).single().getOutcome())
        assertTrue(coveredIds(evaluated).none { IdMapper.isFault(it) })
    }

    @Test
    fun testADriverThatCouldNotPublishSaysWhy() {

        startNcs { AsyncApiReplyDto().apply { published = false; errorMessage = "broker unreachable" } }

        val evaluated = evaluate("bessj", "expint")

        val first = results(evaluated).first()
        assertEquals(AsyncApiOutcome.PUBLISH_FAILED, first.getOutcome())
        assertEquals("broker unreachable", first.getErrorMessage())
        assertTrue(first.stopping)
        assertEquals(1, driver.published.size)
    }

    @Test
    fun testAReplyThatDidNotCarryTheCorrelationIdBackIsRecordedAsSuch() {

        startNcs { replied(DOUBLE_RESULT, correlationMatched = false) }

        val evaluated = evaluate("bessj")

        /*
            From outside there is no telling a defect from a service that correlates by some
            business key instead, so this is recorded, not judged: the reply still counts.
         */
        val result = results(evaluated).single()
        assertEquals(AsyncApiOutcome.REPLIED, result.getOutcome())
        assertEquals(false, result.getCorrelationMatched())
        assertTrue(coveredIds(evaluated).contains("ASYNCAPI_REPLY:doubleResult:bessj"))
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
