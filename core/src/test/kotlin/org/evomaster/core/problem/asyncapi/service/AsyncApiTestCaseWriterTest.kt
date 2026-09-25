package org.evomaster.core.problem.asyncapi.service

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.webfuzzing.asyncapi.access.AsyncApiAccess
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto
import org.evomaster.core.output.TestCase
import org.evomaster.core.output.service.TestCaseWriter
import org.evomaster.core.problem.asyncapi.data.AsyncApiAction
import org.evomaster.core.problem.asyncapi.data.AsyncApiIndividual
import org.evomaster.core.problem.enterprise.SampleType
import org.evomaster.core.problem.rest.builder.RestActionBuilderV3
import org.evomaster.core.search.EvaluatedIndividual
import org.evomaster.core.search.service.FitnessFunction
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.SearchGlobalState
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * What a generated test looks like. The lines that publish are the driver's, so the fake one
 * renders a stand-in for them here, exactly as a Kafka driver would render a producer.
 */
class AsyncApiTestCaseWriterTest {

    companion object {
        private const val NCS = "/asyncapi/sut/ncs-kafka.yaml"

        private const val DOUBLE_RESULT = """{"resultAsDouble": 1.5}"""

        /**
         * What a driver hands over: lines that stand up a client, publish, and read the reply
         * into a variable it names.
         */
        private fun withScript(payload: String, dto: AsyncApiActionDto): AsyncApiReplyDto =
            FakeAsyncApiDriver.replied(payload).apply {
                //named off the variable the core supplied, which is unique per action
                val v = dto.replyVariable
                testScript = listOf(
                    "val producer_$v = connectProducer(\"localhost:9092\")",
                    "producer_$v.send(recordFor(\"${dto.address}\"))",
                    "val $v = pollUntilCorrelated(\"${dto.replyAddress}\")"
                )
            }
    }

    private lateinit var injector: Injector
    private lateinit var driver: FakeAsyncApiDriver
    private lateinit var sampler: AsyncApiSampler
    private lateinit var fitness: FitnessFunction<AsyncApiIndividual>

    @BeforeEach
    fun reset() {
        RestActionBuilderV3.cleanCache()
    }

    private fun start(answer: (AsyncApiActionDto) -> AsyncApiReplyDto?) {
        driver = FakeAsyncApiDriver(AsyncApiTestInjector.sutInfo(AsyncApiAccess.readFromResource(NCS)), answer)
        injector = AsyncApiTestInjector.create(
            driver,
            "--blackBox=false",
            //the core only asks a driver to render lines when a test will be written
            "--createTests=true",
            "--outputFormat=KOTLIN_JUNIT_5"
        )
        sampler = injector.getInstance(AsyncApiSampler::class.java)
        fitness = injector.getInstance(Key.get(object : TypeLiteral<FitnessFunction<AsyncApiIndividual>>() {}))
    }

    private fun evaluate(vararg names: String): EvaluatedIndividual<AsyncApiIndividual> {
        val randomness = injector.getInstance(Randomness::class.java)
        val actions = names.map { name ->
            val template = sampler.seeAvailableActions().first { it.getName() == name }
            (template.copy() as AsyncApiAction).apply { doInitialize(randomness) }
        }
        val individual = AsyncApiIndividual(SampleType.RANDOM, actions.toMutableList()).apply {
            doGlobalInitialize(injector.getInstance(SearchGlobalState::class.java))
        }
        return fitness.calculateCoverage(individual, modifiedSpec = null)
            ?: fail("the fitness gave up on the individual")
    }

    /**
     * The generated body for one test, as the suite writer would ask for it.
     */
    private fun bodyOf(evaluated: EvaluatedIndividual<AsyncApiIndividual>): String {
        val writer = injector.getInstance(TestCaseWriter::class.java)
        return writer.convertToCompilableTestCode(TestCase(evaluated, "test_0"), "baseUrlOfSut").toString()
    }

    @Test
    fun testTheDriversLinesAreWhatPublishes() {

        start { withScript(DOUBLE_RESULT, it) }

        val body = bodyOf(evaluate("bessj"))

        //the driver's own client code, pasted as it was rendered
        assertTrue(body.contains("connectProducer(\"localhost:9092\")"), body)
        assertTrue(body.contains("pollUntilCorrelated"), body)

        //and what only the core knows about the reply
        assertTrue(body.contains("assertNotNull(asyncApiReply_0)"), body)
        assertTrue(body.contains("bessj"), body)
    }

    @Test
    fun testEachActionKeepsItsOwnLines() {

        start { withScript(DOUBLE_RESULT, it) }

        val body = bodyOf(evaluate("bessj", "expint"))

        //the core names the variables, so two actions in one test cannot collide
        assertTrue(body.contains("assertNotNull(asyncApiReply_0)"), body)
        assertTrue(body.contains("assertNotNull(asyncApiReply_1)"), body)
        assertTrue(body.contains("producer_asyncApiReply_0"), body)
        assertTrue(body.contains("producer_asyncApiReply_1"), body)
    }

    @Test
    fun testTheDriverIsToldWhichLanguageToRenderAndWhereToLeaveTheReply() {

        start { withScript(DOUBLE_RESULT, it) }
        evaluate("bessj")

        val asked = driver.published.single()
        assertEquals(SutInfoDto.OutputFormat.KOTLIN_JUNIT_5, asked.outputFormat)
        assertEquals("asyncApiReply_0", asked.replyVariable)
    }

    @Test
    fun testKafkaIsWrittenFromTheContractWhenTheDriverRendersNothing() {

        /*
            The document names the broker, the topics and the header the correlation id rides in,
            so the test can publish with an ordinary client and no driver has to render anything.
         */
        start { FakeAsyncApiDriver.replied(DOUBLE_RESULT) }

        val body = bodyOf(evaluate("bessj"))

        //a real producer and consumer, against the broker the document names
        assertTrue(body.contains("KafkaProducer"), body)
        assertTrue(body.contains("KafkaConsumer"), body)
        assertTrue(body.contains("\"bootstrap.servers\", \"localhost:9092\""), body)
        assertTrue(body.contains("ProducerRecord(\"ncs.bessj.request\""), body)

        //a reply older than this publish is not an answer to it
        assertTrue(body.contains("seekToEnd"), body)

        //a fresh id each run, stamped where the document says and matched on the way back
        assertTrue(body.contains("UUID.randomUUID()"), body)
        assertTrue(body.contains("lastHeader(\"correlationId\")"), body)

        assertTrue(body.contains("assertNotNull(asyncApiReply_0)"), body)
    }

    @Test
    fun testATransportTheContractDoesNotDescribeSaysSoRatherThanWritingAnEmptyTest() {

        /*
            A socket carries its correlation id inside the service's own message layout, which the
            contract does not describe, so nothing here can write the client code. Only a driver
            can, and this one renders nothing.
         */
        val socket = """
            asyncapi: 3.0.0
            info:
              title: Socket
              version: 1.0.0
            servers:
              live:
                host: localhost:8080
                protocol: ws
            channels:
              requests:
                address: /requests
                servers:
                  - ${'$'}ref: '#/servers/live'
                messages:
                  request:
                    payload:
                      type: object
                      properties:
                        id:
                          type: string
            operations:
              ask:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/requests'
        """.trimIndent()

        driver = FakeAsyncApiDriver(AsyncApiTestInjector.sutInfo(socket)) { FakeAsyncApiDriver.fireAndForget() }
        injector = AsyncApiTestInjector.create(
            driver,
            "--blackBox=false",
            "--createTests=true",
            "--outputFormat=KOTLIN_JUNIT_5"
        )
        sampler = injector.getInstance(AsyncApiSampler::class.java)
        fitness = injector.getInstance(Key.get(object : TypeLiteral<FitnessFunction<AsyncApiIndividual>>() {}))

        val body = bodyOf(evaluate("ask"))

        assertTrue(body.contains("not one"), body)
        assertTrue(body.contains("executeAsyncApiAction"), body)
        assertFalse(body.contains("KafkaProducer"), body)
    }

    @Test
    fun testAMessageThatWentUnansweredIsWrittenWithoutAReplyAssertion() {

        start {
            FakeAsyncApiDriver.silence().apply {
                testScript = listOf("publishAndWait()")
            }
        }

        val body = bodyOf(evaluate("bessj"))

        assertTrue(body.contains("publishAndWait()"), body)
        assertTrue(body.contains("NO_REPLY"), body)
        //nothing arrived, so there is nothing to assert on
        assertFalse(body.contains("assertNotNull"), body)
    }
}
