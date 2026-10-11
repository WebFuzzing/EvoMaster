package org.evomaster.core.problem.asyncapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.webfuzzing.asyncapi.access.AsyncApiAccess
import org.evomaster.client.java.controller.api.dto.AdditionalInfoDto
import org.evomaster.client.java.controller.api.dto.EmittedMessageDto
import org.evomaster.client.java.controller.api.dto.StringSpecializationInfoDto
import org.evomaster.client.java.controller.api.dto.TargetInfoDto
import org.evomaster.client.java.controller.api.dto.TestResultsDto
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto
import org.evomaster.client.java.instrumentation.shared.ObjectiveNaming
import org.evomaster.client.java.instrumentation.shared.StringSpecialization
import org.evomaster.client.java.instrumentation.shared.TaintInputName
import org.evomaster.client.java.instrumentation.shared.TaintType
import org.evomaster.core.EMConfig
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
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.IdMapper
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchGlobalState
import org.evomaster.core.search.service.time.ExecutionPhaseController
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AsyncApiFitnessTest {

    companion object {
        private const val NCS = "/asyncapi/sut/ncs-kafka.yaml"

        private const val DOUBLE_RESULT = """{"resultAsDouble": 1.5}"""

        /**
         * Both AsyncAPI fault categories are experimental, so nothing reports them until the
         * user asks for experimental oracles.
         */
        private const val EXPERIMENTAL_ORACLES = "--useExperimentalOracles=true"

        private const val ERROR = """{"error": {"code": 400, "message": "n must be >= 3"}}"""

        private const val BLACK_BOX = "--blackBox"

        /**
         * A line of the SUT, as an instrumented driver would report it once a message reached it.
         */
        private const val SUT_LINE = "${ObjectiveNaming.LINE}_at_com.foo.NcsService_00042"

        /**
         * The numeric id the driver gives [SUT_LINE].
         */
        private const val SUT_LINE_ID = 1

        /**
         * The payload field of [EVENTS] that taint analysis is checked on.
         */
        private const val PAYLOAD_FIELD = "id"

        /**
         * What the service compared the tainted payload field with, in the tests on taint.
         */
        private const val COMPARED_WITH = "order-created"

        /**
         * Where NCS answers bessj, and what it emits there, in the tests on emitted messages.
         */
        private const val BESSJ_REPLY = "ncs.bessj.reply"

        /**
         * A value taint analysis would recognise as its own once the service reports it back.
         */
        private val TAINTED = TaintInputName.getTaintName(0)

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
        /**
         * A shop that consumes orders and is declared to emit a confirmation for each, and
         * events about a user on an address naming that user.
         */
        private val ORDERS = """
            asyncapi: 3.0.0
            info:
              title: Orders
              version: 1.0.0
            servers:
              kafka:
                host: localhost:9092
                protocol: kafka
            channels:
              ordersPlaced:
                address: shop.orders.placed
                messages:
                  orderPlaced:
                    payload:
                      type: object
                      required: [orderId]
                      properties:
                        orderId:
                          type: string
              ordersConfirmed:
                address: shop.orders.confirmed
                messages:
                  orderConfirmed:
                    payload:
                      type: object
                      required: [orderId, status]
                      properties:
                        orderId:
                          type: string
                        status:
                          type: string
                          const: CONFIRMED
              userEvents:
                address: shop.users.{userId}.events
                parameters:
                  userId: {}
                messages:
                  userEvent:
                    payload:
                      type: object
            operations:
              placeOrder:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/ordersPlaced'
              confirmOrder:
                action: send
                channel:
                  ${'$'}ref: '#/channels/ordersConfirmed'
              notifyUser:
                action: send
                channel:
                  ${'$'}ref: '#/channels/userEvents'
        """.trimIndent()
    }

    /**
     * What the shop emits to, in the tests on emitted messages: a declared send channel, one
     * whose address has a placeholder, the channel it consumes from, and somewhere undeclared.
     */
    private object Shop {
        const val CONFIRMED = "shop.orders.confirmed"
        const val CONFIRMED_TARGET = "ASYNCAPI_EMITTED:$CONFIRMED:placeOrder"
        const val CONFIRMED_MESSAGE_TARGET = "ASYNCAPI_EMITTED_MESSAGE:ordersConfirmed.orderConfirmed:placeOrder"
        const val CONFIRMATION = """{"orderId": "o-1", "status": "CONFIRMED"}"""
        const val USER_EVENTS = "shop.users.{userId}.events"
        const val USER_42_EVENTS = "shop.users.42.events"
        const val PLACED = "shop.orders.placed"
        const val UNDECLARED = "shop.audit"
        const val PLACE_ORDER = "placeOrder"
    }

    /**
     * A service that consumes from an address it is also declared to send to, so that a test
     * of its one operation publishes to an address it observes.
     */
    private object Relay {
        const val ADDRESS = "shop.relay"
        const val RELAY = "relay"
        const val TARGET = "ASYNCAPI_EMITTED:$ADDRESS:$RELAY"

        val DOCUMENT = """
            asyncapi: 3.0.0
            info:
              title: Relay
              version: 1.0.0
            channels:
              relay:
                address: $ADDRESS
                messages:
                  hop:
                    payload:
                      type: object
                      properties:
                        n:
                          type: integer
            operations:
              relay:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/relay'
              forward:
                action: send
                channel:
                  ${'$'}ref: '#/channels/relay'
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

    private fun start(
        schemaText: String,
        vararg options: String,
        answer: (AsyncApiActionDto) -> AsyncApiReplyDto?
    ) {

        driver = FakeAsyncApiDriver(AsyncApiTestInjector.sutInfo(schemaText), answer)
        //white-box unless a test asks otherwise: EMConfig refuses an option given twice
        val mode = if (options.any { it.startsWith("$BLACK_BOX=") }) arrayOf() else arrayOf("$BLACK_BOX=false")
        injector = AsyncApiTestInjector.create(driver, *mode, *options)

        sampler = injector.getInstance(AsyncApiSampler::class.java)
        fitness = injector.getInstance(Key.get(object : TypeLiteral<FitnessFunction<AsyncApiIndividual>>() {}))
        idMapper = injector.getInstance(IdMapper::class.java)
    }

    private fun startNcs(vararg options: String, answer: (AsyncApiActionDto) -> AsyncApiReplyDto?) =
        start(AsyncApiAccess.readFromResource(NCS), *options, answer = answer)

    /**
     * The faults an evaluation recorded on its action results, which is where the reports read
     * them from -- as opposed to the targets the search aims at.
     */
    private fun faultsOn(evaluated: EvaluatedIndividual<AsyncApiIndividual>) =
        results(evaluated).flatMap { it.getFaults() }

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

    /**
     * What an instrumented driver reports once a message reached [SUT_LINE].
     */
    private fun lineReached() = TestResultsDto().apply {
        targets.add(TargetInfoDto().apply {
            id = SUT_LINE_ID
            descriptiveId = SUT_LINE
            value = 1.0
            actionIndex = 0
        })
    }

    @Test
    fun testAWhiteBoxRunReadsTheCoverageOfTheService() {

        startNcs { replied(DOUBLE_RESULT) }
        driver.testResults = { lineReached() }

        val evaluated = evaluate("bessj")
        val covered = coveredIds(evaluated)

        assertTrue(driver.testResultsRequests > 0, "the coverage of the service was never asked for")
        assertTrue(covered.contains(SUT_LINE), "$covered")
        assertTrue(covered.contains("ASYNCAPI_OUTCOME:REPLIED:bessj"), "$covered")
    }

    @Test
    fun testABlackBoxRunReadsNoCoverage() {

        /*
            The driver is there only to reach the broker. Even an instrumented one is never
            asked for lines: the targets come from the replies.
         */
        startNcs("$BLACK_BOX=true") { replied(DOUBLE_RESULT) }
        driver.testResults = { lineReached() }

        val evaluated = evaluate("bessj", "expint")
        val covered = coveredIds(evaluated)

        assertEquals(0, driver.testResultsRequests, "a black-box run asked the driver for coverage")
        assertFalse(covered.contains(SUT_LINE), "$covered")
        assertTrue(covered.contains("ASYNCAPI_OUTCOME:REPLIED:bessj"), "$covered")
        assertTrue(covered.contains("ASYNCAPI_REPLY:doubleResult:expint"), "$covered")
        assertEquals(2, driver.published.size)
    }

    /**
     * The [PAYLOAD_FIELD] string gene in the payload of the action at [index].
     */
    private fun payloadField(individual: AsyncApiIndividual, index: Int = 0): StringGene =
        individual.seeMainExecutableActions()[index].seeTopGenes()
            .flatMap { it.flatView() }
            .filterIsInstance<StringGene>()
            .single { it.name == PAYLOAD_FIELD }

    /**
     * What an instrumented driver reports for an action where the service compared [TAINTED]
     * with [COMPARED_WITH].
     */
    private fun comparedWithAConstant() = AdditionalInfoDto().apply {
        stringSpecializations[TAINTED] = listOf(StringSpecializationInfoDto(
            StringSpecialization.CONSTANT.name, COMPARED_WITH, TaintType.FULL_MATCH.name))
    }

    /**
     * Evaluates [count] messages of [EVENTS], the first of which carries [TAINTED] in its payload,
     * with the driver reporting [perAction] as what the service did with its inputs.
     */
    private fun evaluateTainted(count: Int, vararg perAction: AdditionalInfoDto): EvaluatedIndividual<AsyncApiIndividual> {

        driver.testResults = { TestResultsDto().apply { additionalInfoList.addAll(perAction) } }

        val individual = individualOf(*Array(count) { "publishEvent" })
        payloadField(individual).value = TAINTED

        return fitness.calculateCoverage(individual, modifiedSpec = null)
            ?: fail("the fitness gave up on the individual")
    }

    private fun startTheSearch() =
        injector.getInstance(ExecutionPhaseController::class.java).markStartingSearch()

    private fun specializationsOf(gene: StringGene) = gene.specializationGenes.map { it.getValueAsRawString() }

    @Test
    fun testAPayloadStringLearnsWhatTheServiceComparedItWith() {

        start(EVENTS) { fireAndForget() }
        startTheSearch()

        val evaluated = evaluateTainted(1, comparedWithAConstant())

        //the tainted value went out inside the message, which is how the service got to see it
        assertTrue(driver.published.single().payload.contains(TAINTED), driver.published.single().payload)
        assertEquals(listOf(COMPARED_WITH), specializationsOf(payloadField(evaluated.individual)))
    }

    @Test
    fun testATestThatStoppedAtAFailedPublishIsStillAnalysed() {

        /*
            The second publish fails, so the controller reports two entries for three actions.
            That is what a test that stops early looks like, not an inconsistency.
         */
        var sent = 0
        start(EVENTS) {
            sent++
            if (sent == 1) fireAndForget() else AsyncApiReplyDto().apply { published = false }
        }
        startTheSearch()

        val evaluated = evaluateTainted(3, comparedWithAConstant(), AdditionalInfoDto())

        assertEquals(2, driver.published.size)
        assertTrue(results(evaluated)[1].stopping)
        assertEquals(listOf(COMPARED_WITH), specializationsOf(payloadField(evaluated.individual)))
    }

    @Test
    fun testABlackBoxRunNeverRunsTaintAnalysis() {

        /*
            MIO turns taint analysis on even in black-box mode, but there is nothing to analyse
            without asking the driver what the service did, and a black-box run does not ask.
         */
        start(EVENTS, "$BLACK_BOX=true", "--algorithm=MIO") { fireAndForget() }
        startTheSearch()
        assertTrue(injector.getInstance(EMConfig::class.java).isEnabledTaintAnalysis())

        val evaluated = evaluateTainted(1, comparedWithAConstant())

        assertEquals(0, driver.testResultsRequests)
        assertTrue(specializationsOf(payloadField(evaluated.individual)).isEmpty())
    }

    @Test
    fun testTaintAnalysisRunsOnlyDuringTheSearch() {

        start(EVENTS) { fireAndForget() }

        val evaluated = evaluateTainted(1, comparedWithAConstant())

        assertTrue(driver.testResultsRequests > 0)
        assertTrue(specializationsOf(payloadField(evaluated.individual)).isEmpty())
    }

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

        assertEquals(AsyncApiActionDto.CorrelationLocation.HEADER, dto.correlationLocation)
        assertEquals("/correlationId", dto.correlationPointer)
        assertFalse(dto.correlationId.isNullOrBlank())
        assertTrue(dto.headers.isEmpty())
    }

    @Test
    fun testTheSameSeedPublishesTheSameCorrelationIds() {

        /*
            The ids come from the seeded generator, so a run repeated under the same seed sends
            exactly what it sent before. Keeping a reply from an earlier run from being taken for
            this one's is the driver's job, not theirs.
         */
        startNcs { replied(DOUBLE_RESULT) }
        evaluate("bessj", "expint")
        val first = driver.published.map { it.correlationId }

        startNcs { replied(DOUBLE_RESULT) }
        evaluate("bessj", "expint")
        val second = driver.published.map { it.correlationId }

        assertEquals(first, second)
        assertTrue(first.isNotEmpty())
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

        startNcs(EXPERIMENTAL_ORACLES) { replied("""{"something": "else"}""") }

        val evaluated = evaluate("bessj")
        val faults = evaluated.fitness.coveredTargets().filter { idMapper.isFault(it) }

        assertEquals(1, faults.size, coveredIds(evaluated).toString())
        assertTrue(idMapper.isSpecifiedFault(faults.single(), ExperimentalFaultCategory.ASYNCAPI_UNDECLARED_REPLY))
        assertTrue(coveredIds(evaluated).contains("ASYNCAPI_UNRECOGNISED_REPLY:bessj"))
        assertNull(results(evaluated).single().getReplyMessage())

        //the reports count faults off the action result, not off the covered targets
        assertEquals(
            listOf(ExperimentalFaultCategory.ASYNCAPI_UNDECLARED_REPLY),
            faultsOn(evaluated).map { it.category })
    }

    @Test
    fun testSilenceAfterAPromisedReplyIsAFault() {

        startNcs(EXPERIMENTAL_ORACLES) { silence(waited = 5000) }

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

        assertEquals(
            listOf(ExperimentalFaultCategory.ASYNCAPI_NO_REPLY),
            faultsOn(evaluated).map { it.category })
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
        assertEquals(AsyncApiActionDto.CorrelationLocation.HEADER, dto.correlationLocation)
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
        assertEquals(AsyncApiActionDto.CorrelationLocation.PAYLOAD, dto.correlationLocation)
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

    @Test
    fun testAnUnrecognisedReplyIsStillATargetWithoutExperimentalOracles() {

        /*
            The fault is experimental, but the behaviour it reports is not: a reply matching none
            of the declared messages is somewhere the search should still be able to aim, or
            turning the oracle off would quietly cost coverage rather than only reporting.
         */
        startNcs { replied("""{"something": "else"}""") }

        val evaluated = evaluate("bessj")

        assertTrue(
            coveredIds(evaluated).contains("ASYNCAPI_UNRECOGNISED_REPLY:bessj"),
            coveredIds(evaluated).toString())
        assertTrue(evaluated.fitness.coveredTargets().none { idMapper.isFault(it) })
        assertTrue(faultsOn(evaluated).isEmpty())
    }

    @Test
    fun testFaultsAreNotReportedUntilExperimentalOraclesAreAskedFor() {

        //both AsyncAPI categories are experimental, and nothing experimental is on by default
        startNcs { silence() }

        val evaluated = evaluate("bessj")

        assertTrue(coveredIds(evaluated).contains("ASYNCAPI_OUTCOME:NO_REPLY:bessj"))
        assertTrue(evaluated.fitness.coveredTargets().none { idMapper.isFault(it) })
        assertTrue(faultsOn(evaluated).isEmpty())
    }

    @Test
    fun testAReplyWithNoBodyIsNotAnUndeclaredReply() {

        /*
            The contract declares what a reply may be, and one arrived carrying nothing to match
            against it -- an acknowledgement, or a transport that answers in its metadata.
            Nothing was declared to be wrong, so this is not the undeclared-reply fault.
         */
        startNcs(EXPERIMENTAL_ORACLES) { replied(payload = "") }

        val evaluated = evaluate("bessj")

        assertEquals(AsyncApiOutcome.REPLIED, results(evaluated).single().getOutcome())
        assertTrue(evaluated.fitness.coveredTargets().none { idMapper.isFault(it) }, coveredIds(evaluated).toString())
        assertTrue(faultsOn(evaluated).isEmpty())
    }

    @Test
    fun testAnOperationWhoseReplyDeclaresNoMessageIsNotAFault() {

        /*
            A reply channel that carries no message: the contract promises an answer but does not
            say what it looks like, so there is nothing to recognise and nothing to report.
         */
        val document = """
            asyncapi: 3.0.0
            info:
              title: Unspecified reply
              version: 1.0.0
            channels:
              requests:
                address: app.requests
                messages:
                  request:
                    payload:
                      type: object
                      required: [id]
                      properties:
                        id:
                          type: string
              replies:
                address: app.replies
            operations:
              ask:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/requests'
                reply:
                  channel:
                    ${'$'}ref: '#/channels/replies'
        """.trimIndent()

        start(document, EXPERIMENTAL_ORACLES) { replied("""{"anything": 1}""") }

        val evaluated = evaluate("ask")

        assertEquals(AsyncApiOutcome.REPLIED, results(evaluated).single().getOutcome())
        assertNull(results(evaluated).single().getReplyMessage())
        assertTrue(evaluated.fitness.coveredTargets().none { idMapper.isFault(it) }, coveredIds(evaluated).toString())
    }

    @Test
    fun testTheTopicOfAProtocolBindingOverridesTheChannelAddress() {

        /*
            A Kafka channel often names no address of its own and puts the topic in its binding.
            Publishing to the channel's name instead would reach nobody.
         */
        start(AsyncApiAccess.readFromResource("/asyncapi/sut/microcks.yaml")) { fireAndForget() }

        evaluate("receivedServiceChanges")

        assertEquals("microcks-services-updates", driver.published.single().address)
    }

    @Test
    fun testTheReplyTimeoutIsTheConfiguredOne() {

        startNcs("--asyncApiReplyTimeoutMs=1234") { replied(DOUBLE_RESULT) }

        evaluate("bessj")

        assertEquals(1234L, driver.published.single().replyTimeoutMs)
    }

    @Test
    fun testNoSettleWindowIsAskedForByDefault() {

        startNcs { replied(DOUBLE_RESULT) }

        evaluate("bessj")

        assertNull(driver.published.single().settleMs)
    }

    @Test
    fun testTheSettleWindowIsAskedForInWhiteBox() {

        startNcs("--asyncApiSettleMs=250") { fireAndForget() }

        evaluate("bessj", "expint")

        assertEquals(listOf(250L, 250L), driver.published.map { it.settleMs })
        //the whole window unless asked otherwise, and no stable window to go with it
        assertEquals(AsyncApiActionDto.Completion.FIXED, driver.published.first().completion)
        assertNull(driver.published.first().stableMs)
    }

    @Test
    fun testCoverageStableIsAskedForWithItsStableWindow() {

        startNcs("--asyncApiSettleMs=2000", "--asyncApiCompletion=COVERAGE_STABLE", "--asyncApiStableMs=150") {
            fireAndForget()
        }

        evaluate("bessj")

        val dto = driver.published.single()
        assertEquals(2000L, dto.settleMs)
        assertEquals(AsyncApiActionDto.Completion.COVERAGE_STABLE, dto.completion)
        assertEquals(150L, dto.stableMs)
    }

    @Test
    fun testHandlerReturnIsAskedFor() {

        startNcs("--asyncApiSettleMs=2000", "--asyncApiCompletion=HANDLER_RETURN", "--instrumentMR_KAFKA=true") {
            fireAndForget()
        }

        evaluate("bessj")

        val dto = driver.published.single()
        assertEquals(2000L, dto.settleMs)
        assertEquals(AsyncApiActionDto.Completion.HANDLER_RETURN, dto.completion)
        assertNull(dto.stableMs)
    }

    /**
     * What an instrumented driver reports for an action during which the service emitted one
     * message to [address].
     */
    private fun emitted(address: String) = AdditionalInfoDto().apply {
        emittedMessages.add(EmittedMessageDto().apply {
            this.address = address
            payload = DOUBLE_RESULT
            payloadTruncated = false
            payloadType = String::class.java.name
        })
    }

    @Test
    fun testWhatTheServiceEmittedIsKeptOnTheResultOfItsAction() {

        startNcs("--asyncApiSettleMs=2000", "--asyncApiCompletion=HANDLER_RETURN", "--instrumentMR_KAFKA=true") {
            fireAndForget()
        }
        driver.testResults = {
            TestResultsDto().apply {
                additionalInfoList.add(emitted(BESSJ_REPLY))
                //the second message made the service emit nothing
                additionalInfoList.add(AdditionalInfoDto())
            }
        }

        val results = results(evaluate("bessj", "expint"))

        val first = results[0].getEmittedMessages().single()
        assertEquals(BESSJ_REPLY, first.address)
        assertEquals(DOUBLE_RESULT, first.payload)
        assertEquals(false, first.payloadTruncated)
        assertEquals(String::class.java.name, first.payloadType)

        assertTrue(results[1].getEmittedMessages().isEmpty())
        //and a copy of the result keeps them
        assertEquals(BESSJ_REPLY, results[0].copy().getEmittedMessages().single().address)
    }

    @Test
    fun testFewerEntriesThanActionsLeaveTheRestWithoutEmittedMessages() {

        startNcs { fireAndForget() }
        driver.testResults = { TestResultsDto().apply { additionalInfoList.add(emitted(BESSJ_REPLY)) } }

        val results = results(evaluate("bessj", "expint"))

        assertEquals(1, results[0].getEmittedMessages().size)
        assertTrue(results[1].getEmittedMessages().isEmpty())
    }

    @Test
    fun testABlackBoxRunKeepsNoEmittedMessages() {

        startNcs("$BLACK_BOX=true") { fireAndForget() }
        driver.testResults = { TestResultsDto().apply { additionalInfoList.add(emitted(BESSJ_REPLY)) } }

        val result = results(evaluate("bessj")).single()

        assertTrue(result.getEmittedMessages().isEmpty())
    }

    @Test
    fun testABlackBoxRunAsksForNoStableCoverageEither() {

        startNcs("$BLACK_BOX=true") { fireAndForget() }
        //set after start, as EMConfig puts both back to their defaults in black-box
        injector.getInstance(EMConfig::class.java).apply {
            asyncApiSettleMs = 2000
            asyncApiCompletion = EMConfig.AsyncApiCompletion.COVERAGE_STABLE
        }

        evaluate("bessj")

        val dto = driver.published.single()
        assertNull(dto.settleMs)
        assertNull(dto.completion)
        assertNull(dto.stableMs)
    }

    @Test
    fun testWhyTheControllerStoppedWaitingIsKeptOnTheResult() {

        startNcs("--asyncApiSettleMs=2000", "--asyncApiCompletion=COVERAGE_STABLE") {
            fireAndForget().apply {
                settledMs = 230
                settleEndedBy = AsyncApiReplyDto.SettleEnd.COVERAGE_STABLE
            }
        }

        val result = results(evaluate("bessj")).single()

        assertEquals(230L, result.getSettledMs())
        assertEquals(AsyncApiReplyDto.SettleEnd.COVERAGE_STABLE, result.getSettleEndedBy())
    }

    @Test
    fun testABlackBoxRunAsksForNoSettleWindow() {

        /*
            The window is there so that the code a message reaches is credited to it. A
            black-box run collects no code coverage, so it would only slow the search down.
         */
        startNcs("$BLACK_BOX=true") { fireAndForget() }
        //set after start, as EMConfig puts it back to 0 in black-box
        injector.getInstance(EMConfig::class.java).asyncApiSettleMs = 250

        evaluate("bessj")

        assertNull(driver.published.single().settleMs)
    }

    @Test
    fun testHowLongTheControllerWaitedIsKeptOnTheResult() {

        startNcs("--asyncApiSettleMs=250") { fireAndForget().apply { settledMs = 251 } }

        val evaluated = evaluate("bessj")

        assertEquals(251L, results(evaluated).single().getSettledMs())
    }

    @Test
    fun testNoWaitIsRecordedWhenTheControllerReportsNone() {

        startNcs { fireAndForget() }

        val evaluated = evaluate("bessj")

        assertNull(results(evaluated).single().getSettledMs())
        assertNull(results(evaluated).single().getSettleEndedBy())
    }

    /**
     * What an instrumented driver reports for an action during which the service emitted
     * [payload], or something it could only name by [payloadType], to each of [addresses].
     */
    private fun emittedTo(vararg addresses: String, payload: String? = Shop.CONFIRMATION, payloadType: String? = null) =
        AdditionalInfoDto().apply {
            addresses.forEach { a ->
                emittedMessages.add(EmittedMessageDto().apply {
                    address = a
                    this.payload = payload
                    payloadTruncated = false
                    this.payloadType = payloadType ?: String::class.java.name
                })
            }
        }

    private fun startShop(perAction: List<AdditionalInfoDto>, vararg options: String) {
        start(ORDERS, *options) { fireAndForget() }
        driver.testResults = { TestResultsDto().apply { perAction.forEach { additionalInfoList.add(it) } } }
    }

    @Test
    fun testEmittingToADeclaredSendChannelCoversItsTarget() {

        startShop(listOf(emittedTo(Shop.CONFIRMED)))

        val evaluated = evaluate(Shop.PLACE_ORDER)
        val covered = coveredIds(evaluated)

        assertTrue(covered.contains(Shop.CONFIRMED_TARGET), "$covered")
        //and the payload is the message declared for that channel
        assertTrue(covered.contains(Shop.CONFIRMED_MESSAGE_TARGET), "$covered")
        assertEquals(listOf(Shop.CONFIRMED), results(evaluated).single().getDeclaredEmissionAddresses())
        assertTrue(faultsOn(evaluated).isEmpty())
    }

    @Test
    fun testAPayloadThatIsNoDeclaredMessageStillCoversTheAddress() {

        startShop(listOf(emittedTo(Shop.CONFIRMED, payload = """{"orderId": "o-1"}""")), EXPERIMENTAL_ORACLES)

        val evaluated = evaluate(Shop.PLACE_ORDER)
        val covered = coveredIds(evaluated)

        assertTrue(covered.contains(Shop.CONFIRMED_TARGET), "$covered")
        assertFalse(covered.contains(Shop.CONFIRMED_MESSAGE_TARGET), "$covered")
        //what a service sends is often described loosely: not a fault
        assertTrue(faultsOn(evaluated).isEmpty())
    }

    @Test
    fun testAPayloadThatWasNotTextIsNotClassified() {

        startShop(listOf(emittedTo(Shop.CONFIRMED, payload = null, payloadType = "com.foo.OrderConfirmed")))

        val covered = coveredIds(evaluate(Shop.PLACE_ORDER))

        assertTrue(covered.contains(Shop.CONFIRMED_TARGET), "$covered")
        assertFalse(covered.contains(Shop.CONFIRMED_MESSAGE_TARGET), "$covered")
    }

    @Test
    fun testAnAddressWithAPlaceholderIsMatchedAndNamedAsTheDocumentWritesIt() {

        startShop(listOf(emittedTo(Shop.USER_42_EVENTS, payload = "{}")))

        val evaluated = evaluate(Shop.PLACE_ORDER)
        val covered = coveredIds(evaluated)

        assertTrue(covered.contains("ASYNCAPI_EMITTED:${Shop.USER_EVENTS}:${Shop.PLACE_ORDER}"), "$covered")
        assertTrue(covered.contains("ASYNCAPI_EMITTED_MESSAGE:userEvents.userEvent:${Shop.PLACE_ORDER}"), "$covered")
        //what a generated test observes is the concrete topic
        assertEquals(listOf(Shop.USER_42_EVENTS), results(evaluated).single().getDeclaredEmissionAddresses())
    }

    @Test
    fun testEmittingToAnUndeclaredAddressIsAFaultOnlyWithExperimentalOracles() {

        startShop(listOf(emittedTo(Shop.UNDECLARED)))
        val quiet = evaluate(Shop.PLACE_ORDER)
        assertTrue(quiet.fitness.coveredTargets().none { idMapper.isFault(it) }, coveredIds(quiet).toString())
        assertTrue(faultsOn(quiet).isEmpty())
        assertTrue(coveredIds(quiet).none { it.startsWith("ASYNCAPI_EMITTED") }, coveredIds(quiet).toString())

        startShop(listOf(emittedTo(Shop.UNDECLARED)), EXPERIMENTAL_ORACLES)
        val evaluated = evaluate(Shop.PLACE_ORDER)

        val fault = faultsOn(evaluated).single()
        assertEquals(ExperimentalFaultCategory.ASYNCAPI_UNDECLARED_EMISSION, fault.category)
        assertEquals(Shop.PLACE_ORDER, fault.operationId)
        assertEquals(Shop.UNDECLARED, fault.context)
        val faults = evaluated.fitness.coveredTargets().filter { idMapper.isFault(it) }
        assertTrue(idMapper.isSpecifiedFault(faults.single(), ExperimentalFaultCategory.ASYNCAPI_UNDECLARED_EMISSION))
        assertTrue(results(evaluated).single().getDeclaredEmissionAddresses().isEmpty())
    }

    @Test
    fun testTwoUndeclaredAddressesAreTwoFaults() {

        startShop(listOf(emittedTo(Shop.UNDECLARED, "shop.other")), EXPERIMENTAL_ORACLES)

        val evaluated = evaluate(Shop.PLACE_ORDER)

        assertEquals(setOf(Shop.UNDECLARED, "shop.other"), faultsOn(evaluated).map { it.context }.toSet())
        assertEquals(2, evaluated.fitness.coveredTargets().count { idMapper.isFault(it) })
    }

    @Test
    fun testTheUndeclaredAddressesOfAnOperationAreCapped() {

        val addresses = (0..10).map { "shop.user.$it" }
        startShop(listOf(emittedTo(*addresses.toTypedArray())), EXPERIMENTAL_ORACLES)

        val evaluated = evaluate(Shop.PLACE_ORDER)

        //the first ten in the order emitted, the eleventh is not reported
        assertEquals(addresses.take(10), faultsOn(evaluated).map { it.context })
        assertEquals(10, evaluated.fitness.coveredTargets().count { idMapper.isFault(it) })

        //one already reported still is, in a later test
        driver.testResults = { TestResultsDto().apply { additionalInfoList.add(emittedTo(addresses[0], addresses[10])) } }
        val again = evaluate(Shop.PLACE_ORDER)
        assertEquals(listOf(addresses[0]), faultsOn(again).map { it.context })
    }

    @Test
    fun testWhatWasEmittedWhileAMessageFailedToBePublishedIsNotCreditedToIt() {

        var calls = 0
        start(ORDERS, EXPERIMENTAL_ORACLES) {
            if (calls++ == 0) fireAndForget() else AsyncApiReplyDto().apply { published = false }
        }
        driver.testResults = {
            TestResultsDto().apply {
                additionalInfoList.add(AdditionalInfoDto())
                //late work of the first message, recorded while the second one was being tried
                additionalInfoList.add(emittedTo(Shop.CONFIRMED, Shop.UNDECLARED))
            }
        }

        val evaluated = evaluate(Shop.PLACE_ORDER, Shop.PLACE_ORDER)
        val results = results(evaluated)

        assertEquals(AsyncApiOutcome.PUBLISH_FAILED, results[1].getOutcome())
        val covered = coveredIds(evaluated)
        assertFalse(covered.contains(Shop.CONFIRMED_TARGET), "$covered")
        assertTrue(faultsOn(evaluated).isEmpty())
        assertTrue(results[1].getEmittedMessages().isEmpty())
        assertTrue(results[1].getDeclaredEmissionAddresses().isEmpty())
    }

    @Test
    fun testTheAddressAMessageIsPublishedToIsNotObservedByItsTest() {

        start(Relay.DOCUMENT) { fireAndForget() }
        driver.testResults = { TestResultsDto().apply { additionalInfoList.add(emittedTo(Relay.ADDRESS)) } }

        val evaluated = evaluate(Relay.RELAY)

        //the service emitting there is still something it did
        val covered = coveredIds(evaluated)
        assertTrue(covered.contains(Relay.TARGET), "$covered")
        //but a test observing that address would collect its own message too
        assertTrue(results(evaluated).single().getDeclaredEmissionAddresses().isEmpty())
    }

    @Test
    fun testEmittingToAChannelTheServiceConsumesIsNeitherATargetNorAFault() {

        startShop(listOf(emittedTo(Shop.PLACED)), EXPERIMENTAL_ORACLES)

        val evaluated = evaluate(Shop.PLACE_ORDER)

        assertTrue(coveredIds(evaluated).none { it.startsWith("ASYNCAPI_EMITTED") }, coveredIds(evaluated).toString())
        assertTrue(faultsOn(evaluated).isEmpty())
    }

    @Test
    fun testAReplyIsNotCountedAgainAsAnEmittedMessage() {

        /*
            NCS answers on the reply channel of the operation published to, which the reply
            targets already cover: no emitted target, and no fault, as the channel is declared.
            The same for a reply on another operation's reply channel.
         */
        startNcs(EXPERIMENTAL_ORACLES) { replied(DOUBLE_RESULT) }
        driver.testResults = {
            TestResultsDto().apply { additionalInfoList.add(emittedTo(BESSJ_REPLY, "ncs.expint.reply", payload = DOUBLE_RESULT)) }
        }

        val evaluated = evaluate("bessj")
        val covered = coveredIds(evaluated)

        assertTrue(covered.contains("ASYNCAPI_REPLY:doubleResult:bessj"), "$covered")
        assertTrue(covered.none { it.startsWith("ASYNCAPI_EMITTED") }, "$covered")
        assertTrue(faultsOn(evaluated).isEmpty(), faultsOn(evaluated).toString())
        assertTrue(results(evaluated).single().getDeclaredEmissionAddresses().isEmpty())
        //still kept, as everything the service emitted is
        assertEquals(2, results(evaluated).single().getEmittedMessages().size)
    }

    @Test
    fun testEachActionCoversTheTargetsOfWhatItMadeTheServiceEmit() {

        startShop(listOf(AdditionalInfoDto(), emittedTo(Shop.CONFIRMED, Shop.CONFIRMED)))

        val evaluated = evaluate(Shop.PLACE_ORDER, Shop.PLACE_ORDER)
        val target = evaluated.fitness.coveredTargets().single { idMapper.getDescriptiveId(it) == Shop.CONFIRMED_TARGET }

        //credited to the second message, the one that made the service emit
        assertTrue(evaluated.fitness.getTargetsByAction(1).contains(target))
        assertFalse(evaluated.fitness.getTargetsByAction(0).contains(target))
        val results = results(evaluated)
        assertTrue(results[0].getDeclaredEmissionAddresses().isEmpty())
        //one address, however many times it was emitted to
        assertEquals(listOf(Shop.CONFIRMED), results[1].getDeclaredEmissionAddresses())
    }

    @Test
    fun testABlackBoxRunCoversNoEmittedTargets() {

        startShop(listOf(emittedTo(Shop.CONFIRMED, Shop.UNDECLARED)), "$BLACK_BOX=true", EXPERIMENTAL_ORACLES)

        val evaluated = evaluate(Shop.PLACE_ORDER)

        assertTrue(coveredIds(evaluated).none { it.startsWith("ASYNCAPI_EMITTED") }, coveredIds(evaluated).toString())
        assertTrue(faultsOn(evaluated).isEmpty())
    }
}
