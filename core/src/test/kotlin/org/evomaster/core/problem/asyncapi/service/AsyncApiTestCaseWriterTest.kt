package org.evomaster.core.problem.asyncapi.service

import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.TypeLiteral
import com.webfuzzing.asyncapi.access.AsyncApiAccess
import org.evomaster.client.java.controller.api.dto.SutInfoDto
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiActionDto
import org.evomaster.client.java.controller.api.dto.problem.asyncapi.AsyncApiReplyDto
import org.evomaster.core.output.TestCase
import org.evomaster.core.output.TestSuiteSplitter
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.output.Termination
import org.evomaster.core.output.compiler.CompilerForTestGenerated
import org.evomaster.core.output.service.KafkaTestClientEmitter
import org.evomaster.core.search.Solution
import org.evomaster.core.EMConfig
import org.evomaster.core.output.service.TestSuiteWriter
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
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * What a generated test looks like. The lines that publish are the driver's, so the fake one
 * renders a stand-in for them here, exactly as a Kafka driver would render a producer.
 */
class AsyncApiTestCaseWriterTest {

    companion object {
        private const val DOUBLE_RESULT = """{"resultAsDouble": 1.5}"""

        /**
         * A document that reaches what the shared one cannot.
         *
         * NCS answers every operation and names its correlation header exactly what the default
         * is, so three of the arguments a call carries are never seen as anything else: a
         * message with no reply at all, a header named something of the service's own choosing,
         * and headers of the message itself.
         */
        private val VARIANTS = """
            asyncapi: 3.0.0
            info:
              title: Variants
              version: 1.0.0
            servers:
              broker:
                host: broker.local:19092
                protocol: kafka
            channels:
              askRequest:
                address: ask.request
                servers:
                  - ${'$'}ref: '#/servers/broker'
                messages:
                  askRequest:
                    correlationId:
                      location: '${'$'}message.header#/x-corr-id'
                    headers:
                      type: object
                      required: [tenant]
                      properties:
                        tenant:
                          type: string
                          const: acme
                    payload:
                      type: object
                      required: [value]
                      properties:
                        value:
                          type: integer
              askReply:
                address: ask.reply
                servers:
                  - ${'$'}ref: '#/servers/broker'
                messages:
                  answer:
                    payload:
                      type: object
                      required: [answer]
                      properties:
                        answer:
                          type: integer
              tellRequest:
                address: tell.request
                servers:
                  - ${'$'}ref: '#/servers/broker'
                messages:
                  tellRequest:
                    payload:
                      type: object
                      required: [note]
                      properties:
                        note:
                          type: string
                          const: hello
            operations:
              ask:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/askRequest'
                reply:
                  channel:
                    ${'$'}ref: '#/channels/askReply'
              tell:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/tellRequest'
        """.trimIndent()

        /**
         * Every line any other suite opens with, taken verbatim from what the writer produces
         * for a suite with no tests in it (see TestSuiteWriterTest). The class name and the
         * driver's own name are left out, as those are all that may differ.
         */
        private val SCAFFOLDING_EVERY_SUITE_HAS = listOf(
            "/**",
            "* LICENSE DISCLAIMER",
            "* This file has been generated by EvoMaster.",
            "* The content of this file is not subject to the license of EvoMaster itself, i.e., LGPL.",
            "* This generated software (i.e., the test suite in this file) can be freely used, modified,",
            "* and distributed as you see fit without any restrictions.",
            "*/",
            "import  org.junit.jupiter.api.AfterAll",
            "import  org.junit.jupiter.api.BeforeAll",
            "import  org.junit.jupiter.api.BeforeEach",
            "import  org.junit.jupiter.api.Test",
            "import  org.junit.jupiter.api.Timeout",
            "import  org.junit.jupiter.api.Assertions.*",
            "import  java.util.List",
            "import  org.evomaster.test.utils.EMTestUtils.*",
            "import  org.evomaster.client.java.controller.SutHandler",
            "import  java.math.BigDecimal",
            "import  java.math.BigInteger",
            "import  org.hamcrest.Matchers",
            "import  org.hamcrest.Matchers.*",
            "import  org.evomaster.client.java.controller.contentMatchers.NumberMatcher.*",
            "import  org.evomaster.client.java.controller.contentMatchers.StringMatcher.*",
            "import  org.evomaster.client.java.controller.contentMatchers.SubStringMatcher.*",
            "    companion object {",
            "        private lateinit var baseUrlOfSut: String",
            "        @BeforeAll",
            "        @JvmStatic",
            "        fun initClass() {",
            "            controller.setupForGeneratedTest()",
            "            baseUrlOfSut = controller.startSut()",
            "            controller.registerOrExecuteInitSqlCommandsIfNeeded()",
            "            assertNotNull(baseUrlOfSut)",
            "        }",
            "        @AfterAll",
            "        @JvmStatic",
            "        fun tearDown() {",
            "            controller.stopSut()",
            "        }",
            "    }",
            "    @BeforeEach",
            "    fun initTest() {",
            "        controller.resetStateOfSUT()",
            "    }"
        )

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

    private fun start(vararg options: String, answer: (AsyncApiActionDto) -> AsyncApiReplyDto?) {
        startWith(AsyncApiAccess.readFromResource(AsyncApiTestInjector.NCS), *options, answer = answer)
    }

    /**
     * The same, over a document written for one test rather than the shared one.
     */
    private fun startWith(
        document: String,
        vararg options: String,
        answer: (AsyncApiActionDto) -> AsyncApiReplyDto?
    ) {
        driver = FakeAsyncApiDriver(AsyncApiTestInjector.sutInfo(document), answer)

        //what a test did not ask for, so that asking for it does not pass the option twice
        val defaults = listOf(
            "--blackBox=false",
            //the core only asks a driver to render lines when a test will be written
            "--createTests=true",
            "--outputFormat=KOTLIN_JUNIT_5"
        ).filterNot { d -> options.any { it.substringBefore('=') == d.substringBefore('=') } }

        injector = AsyncApiTestInjector.create(driver, *(defaults + options).toTypedArray())
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
     * The members the writer adds to the test class for a solution holding this one test.
     */
    private fun membersOf(evaluated: EvaluatedIndividual<AsyncApiIndividual>): String {
        val writer = injector.getInstance(TestCaseWriter::class.java)
        val solution = Solution(mutableListOf(evaluated), "Prefix", "Suffix", Termination.NONE, listOf(), listOf())
        val lines = Lines(injector.getInstance(EMConfig::class.java).outputFormat)
        writer.addExtraClassMembers(lines, solution)
        return lines.toString()
    }

    /**
     * Both halves, in the order the suite writer produces them: the members first, as that is
     * what tells the writer which servers the suite has. Asking for a body first would write one
     * that publishes to the document's address even where a driver could have been asked.
     */
    private fun membersAndBodyOf(evaluated: EvaluatedIndividual<AsyncApiIndividual>): Pair<String, String> {
        val members = membersOf(evaluated)
        return members to bodyOf(evaluated)
    }

    /**
     * Whether what was emitted is a program at all.
     *
     * The suite as written calls a Kafka client, which core does not depend on and must not: the
     * dependency belongs to the generated suite, not to the fuzzer. So the helper is replaced by
     * a declaration of the same shape, and what is compiled is everything the writer decided --
     * the call, its arguments, the escaping of the payload, the literals and the assertions.
     */
    private fun compiles(members: String, body: String, format: OutputFormat, name: String) {

        val servers = Regex("${KafkaTestClientEmitter.SERVER_VARIABLE_PREFIX}\\w+")
            .findAll(members + body)
            .map { it.value }
            .toSet()

        val indented = body.trimEnd().lines().joinToString("\n") { "    $it" }

        val code = if (format.isJava()) {
            buildString {
                append("import org.junit.jupiter.api.Test;\n")
                append("import org.junit.jupiter.api.Timeout;\n")
                append("import static org.junit.jupiter.api.Assertions.*;\n\n")
                append("public class $name {\n")
                servers.forEach { append("    private String $it = \"localhost:9092\";\n") }
                append("    private String ${KafkaTestClientEmitter.HELPER_NAME}(String broker, String topic,")
                append(" String replyTopic, String payload, String correlationHeader, long timeoutMs,")
                append(" String... headerPairs) throws Exception { return null; }\n")
                append(indented).append("\n}\n")
            }
        } else {
            buildString {
                append("import org.junit.jupiter.api.Test\n")
                append("import org.junit.jupiter.api.Timeout\n")
                append("import org.junit.jupiter.api.Assertions.*\n\n")
                append("internal class $name {\n")
                servers.forEach { append("    private val $it: String = \"localhost:9092\"\n") }
                append("    private fun ${KafkaTestClientEmitter.HELPER_NAME}(broker: String, topic: String,")
                append(" replyTopic: String?, payload: String?, correlationHeader: String?, timeoutMs: Long,")
                append(" vararg headerPairs: String): String? = null\n")
                append(indented).append("\n}\n")
            }
        }

        CompilerForTestGenerated.compile(format, code, name)
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
        assertTrue(body.contains("assertNotNull(res_0)"), body)
        assertTrue(body.contains("bessj: REPLIED"), body)
    }

    @Test
    fun testEachActionKeepsItsOwnLines() {

        start { withScript(DOUBLE_RESULT, it) }

        val body = bodyOf(evaluate("bessj", "expint"))

        //the core names the variables, so two actions in one test cannot collide
        assertTrue(body.contains("assertNotNull(res_0)"), body)
        assertTrue(body.contains("assertNotNull(res_1)"), body)
        assertTrue(body.contains("producer_res_0"), body)
        assertTrue(body.contains("producer_res_1"), body)
    }

    @Test
    fun testTheDriverIsToldWhichLanguageToRenderAndWhereToLeaveTheReply() {

        start { withScript(DOUBLE_RESULT, it) }
        evaluate("bessj")

        val asked = driver.published.single()
        assertEquals(SutInfoDto.OutputFormat.KOTLIN_JUNIT_5, asked.outputFormat)
        assertEquals("res_0", asked.replyVariable)
    }

    @Test
    fun testKafkaIsWrittenFromTheContractWhenTheDriverRendersNothing() {

        /*
            The document names the broker, the topics and the header the correlation id rides in,
            so the test can publish with an ordinary client and no driver has to render anything.
         */
        start { FakeAsyncApiDriver.replied(DOUBLE_RESULT) }

        val evaluated = evaluate("bessj")

        /*
            The members first, as the suite writer writes them: that is where the servers of a
            suite are learnt, and a body written before them would not know they exist.
         */
        val members = membersOf(evaluated)
        assertTrue(members.contains("KafkaProducer"), members)
        assertTrue(members.contains("KafkaConsumer"), members)
        //a reply older than the test is not an answer to it, and each run stamps its own id
        assertTrue(members.contains("seekToEnd"), members)
        assertTrue(members.contains("UUID.randomUUID()"), members)

        /*
            Seeking to the end is lazy in every client written so far: it takes effect on the
            first poll, by which time the reply is behind it. Asking for the position is what
            prevents that, and it is the one line a real broker was needed to find missing.
         */
        assertTrue(members.contains("c.position(parts[0])"), members)

        //and the clients are closed however the call ends, not only when it succeeds
        assertTrue(members.contains("} finally {"), members)
        assertTrue(members.contains("producer?.close()"), members)
        assertTrue(members.contains("consumer?.close()"), members)
        assertTrue(members.contains("asyncApiServer_kafka"), members)

        val body = bodyOf(evaluated)

        //one line per action: everything it needs came off the contract
        assertTrue(body.contains("${KafkaTestClientEmitter.HELPER_NAME}(asyncApiServer_kafka,"), body)
        assertTrue(body.contains("\"ncs.bessj.request\""), body)
        assertTrue(body.contains("\"ncs.bessj.reply\""), body)
        assertTrue(body.contains("\"correlationId\""), body)
        assertTrue(body.contains("assertNotNull(res_0)"), body)
        //the suite has a driver, so the document's address is not what a test publishes to
        assertFalse(body.contains("\"localhost:9092\""), body)
    }

    /**
     * A whole suite, written to disk by the shared suite writer and read back, which is the
     * only way to reach what it puts around the tests: the class, its lifecycle, and whether
     * there is a driver in it at all.
     */
    private fun writeSuite(extension: String, vararg options: String): String {

        val folder = java.nio.file.Files.createTempDirectory("asyncapi_suite").toFile()
        folder.deleteOnExit()

        startWith(AsyncApiAccess.readFromResource(AsyncApiTestInjector.NCS), *options) {
            FakeAsyncApiDriver.replied(DOUBLE_RESULT)
        }

        val config = injector.getInstance(EMConfig::class.java)
        config.outputFolder = folder.absolutePath
        config.outputFilePrefix = "AsyncApiSuite"
        config.outputFileSuffix = ""

        val evaluated = evaluate("bessj")
        val solution = Solution(mutableListOf(evaluated), "AsyncApiSuite", "", Termination.NONE, listOf(), listOf())

        injector.getInstance(TestSuiteWriter::class.java)
            .writeTests(solution, FakeAsyncApiDriver::class.qualifiedName!!, null)

        //by the prefix too: a Python suite is written beside the utilities it imports
        return folder.walkTopDown()
            .first { it.isFile && it.name == "AsyncApiSuite$extension" }
            .readText()
    }

    @Test
    fun testThePythonSuiteIsValidPython() {

        /*
            A Python suite has no driver to ask where the broker is, since the controller is
            Java, so it publishes to the address the document declares. What matters here is
            that the file the run writes parses: indentation is part of the language, so a
            helper written as text can break it in a way no JVM format can.

            The whole suite is written and checked, imports included, rather than a module
            assembled here: whether those imports are emitted at all is itself a decision the
            writer makes, and one this would otherwise be making for it.
         */
        val written = writeSuite(".py", "--blackBox=true", "--outputFormat=PYTHON_UNITTEST")

        //the call, and the reply read as a dict rather than a tree of nodes
        assertTrue(written.contains("${KafkaTestClientEmitter.HELPER_NAME}("), written)
        assertTrue(written.contains("json.loads(res_0)"), written)
        assertTrue(written.contains("\"resultAsDouble\" in body_0"), written)

        //the helper it calls, and the imports that helper needs
        assertTrue(written.contains("kafka.KafkaProducer"), written)
        assertTrue(written.contains("import kafka"), written)
        assertTrue(written.contains("import uuid"), written)
        assertTrue(written.contains("seek_to_end"), written)

        /*
            Seeking to the end is lazy in every client written so far: it takes effect on the
            first poll, by which time the reply is behind it and is never seen. A suite that
            only has to parse cannot catch that.
         */
        assertTrue(written.contains("consumer.position(partitions[0])"), written)

        //there is no driver in a Python suite, so nothing is declared to ask one
        assertFalse(written.contains(KafkaTestClientEmitter.SERVER_VARIABLE_PREFIX), written)

        //and the driver was asked for no script, as its own enum cannot name Python
        assertNull(driver.published.last().outputFormat)

        parsesAsPython(written)
    }

    /**
     * Whether python3 can read the file the run wrote. Skipped, visibly, where there is none.
     */
    private fun parsesAsPython(suite: String) {

        val file = java.io.File.createTempFile("asyncapi_suite", ".py")
        file.deleteOnExit()
        file.writeText(suite)

        try {
            val process = try {
                ProcessBuilder("python3", "-m", "py_compile", file.absolutePath)
                    .redirectErrorStream(true)
                    .start()
            } catch (e: java.io.IOException) {
                //no Python on this machine: the rest of the suite has nothing to do with one
                Assumptions.assumeTrue(false, "python3 is not on the PATH")
                return
            }
            val output = process.inputStream.bufferedReader().readText()
            val code = process.waitFor()

            assertEquals(0, code, "the generated Python does not parse:\n$output\n\n$suite")
        } finally {
            file.delete()
        }
    }

    @Test
    fun testABlackBoxSuiteHasNoDriverAndPublishesWhereTheDocumentSays() {

        /*
            Black-box is the mode this problem type is unusual in: a driver publishes while the
            search runs, because nothing else can, but the suite that run leaves behind has no
            driver in it. So there is nobody to ask where the broker is, and the only address
            there is is the one the document declares.
         */
        val written = writeSuite(".kt", "--blackBox=true", "--outputFormat=KOTLIN_JUNIT_5")

        /*
            Nothing of the driver: no field holding it, and nothing started or stopped. The type
            is still imported, as it is in every suite the shared writer produces, so what says
            there is no driver is that no line both names it and assigns one.
         */
        assertFalse(written.lines().any { it.contains("SutHandler") && it.contains("=") }, written)
        assertFalse(written.contains("controller.startSut()"), written)
        assertFalse(written.contains("controller.stopSut()"), written)
        assertFalse(written.contains("getAsyncApiServerAddress"), written)
        assertFalse(written.contains(KafkaTestClientEmitter.SERVER_VARIABLE_PREFIX), written)

        //and so the document's own address, as a literal, where the variable would have been
        assertTrue(
            written.contains("${KafkaTestClientEmitter.HELPER_NAME}(\"localhost:9092\", \"ncs.bessj.request\""),
            written
        )

        //the rest is what any other suite has
        assertTrue(written.contains("This file has been generated by EvoMaster"), written)
        assertTrue(written.contains("internal class AsyncApiSuite {"), written)
        assertTrue(written.contains("fun test_0_publishOnBessj"), written)
        assertTrue(written.contains("assertNotNull(res_0)"), written)
    }

    @Test
    fun testABlackBoxJavaSuiteFallsBackTheSameWay() {

        //the same, in the other language a JVM suite can be written in
        val written = writeSuite(".java", "--blackBox=true", "--outputFormat=JAVA_JUNIT_5")

        assertFalse(written.lines().any { it.contains("SutHandler") && it.contains("=") }, written)
        assertFalse(written.contains(KafkaTestClientEmitter.SERVER_VARIABLE_PREFIX), written)
        assertTrue(
            written.contains("${KafkaTestClientEmitter.HELPER_NAME}(\"localhost:9092\", \"ncs.bessj.request\""),
            written
        )
        assertTrue(written.contains("public class AsyncApiSuite {"), written)
    }

    @Test
    fun testTheWholeSuiteHasTheSameScaffoldingAsAnyOther() {

        /*
            The class, its lifecycle and the driver it starts come from the shared suite writer,
            so an AsyncAPI suite has to look like any other: only what a test does inside its own
            body is ours. Written to disk and read back, rather than asserted on fragments.
         */
        val folder = java.nio.file.Files.createTempDirectory("asyncapi_suite").toFile()
        folder.deleteOnExit()

        start { FakeAsyncApiDriver.replied(DOUBLE_RESULT) }

        val config = injector.getInstance(EMConfig::class.java)
        config.outputFolder = folder.absolutePath
        config.outputFilePrefix = "AsyncApiSuite"
        config.outputFileSuffix = ""

        val evaluated = evaluate("bessj")
        val solution = Solution(mutableListOf(evaluated), "AsyncApiSuite", "", Termination.NONE, listOf(), listOf())

        injector.getInstance(TestSuiteWriter::class.java)
            .writeTests(solution, FakeAsyncApiDriver::class.qualifiedName!!, null)

        val written = folder.walkTopDown().first { it.isFile && it.name.endsWith(".kt") }.readText()

        val actual = written.lines().map { it.trimEnd() }

        /*
            Every line another suite opens with has to be here, in the same order and with the
            same indentation. Extra lines in between are allowed: that is where what a suite of
            this problem type adds goes.
         */
        var from = 0
        SCAFFOLDING_EVERY_SUITE_HAS.forEach { line ->
            val at = actual.subList(from, actual.size).indexOf(line)
            assertTrue(at >= 0, "missing, or out of order: '" + line + "'\n\n" + written)
            from += at + 1
        }

        //the class and the driver it holds, which only differ by their names
        assertTrue(actual.contains("internal class AsyncApiSuite {"), written)
        assertTrue(actual.any { it.startsWith("        private val controller : SutHandler = ") }, written)

        /*
            And ours on top of it, not instead of it. Pinned whole: through a prefix, dropping
            the fallback would still pass, and a lateinit var assigned null fails every test in
            the suite at initClass.
         */
        assertTrue(
            actual.contains("            asyncApiServer_kafka = controller.getAsyncApiServerAddress(\"kafka\") ?: \"localhost:9092\""),
            written
        )
        assertTrue(written.contains("publishOnBessj"), written)

        folder.deleteRecursively()
    }

    @Test
    fun testTheDriverIsAskedForAnAddressOnlyOnce() {

        /*
            Java has no elvis, and the obvious ternary would name the call on both sides of it.
            A driver that works the address out, rather than keeping one, would then do that
            work twice every time a suite starts.
         */
        driver = FakeAsyncApiDriver(AsyncApiTestInjector.sutInfo(AsyncApiAccess.readFromResource(AsyncApiTestInjector.NCS))) {
            FakeAsyncApiDriver.replied(DOUBLE_RESULT)
        }
        injector = AsyncApiTestInjector.create(
            driver,
            "--blackBox=false",
            "--createTests=true",
            "--outputFormat=JAVA_JUNIT_5"
        )
        sampler = injector.getInstance(AsyncApiSampler::class.java)
        fitness = injector.getInstance(Key.get(object : TypeLiteral<FitnessFunction<AsyncApiIndividual>>() {}))

        val evaluated = evaluate("bessj")
        val writer = injector.getInstance(TestCaseWriter::class.java)
        val solution = Solution(mutableListOf(evaluated), "Prefix", "Suffix", Termination.NONE, listOf(), listOf())

        //declaring the members is what tells the writer which servers there are to ask about
        writer.addExtraClassMembers(Lines(OutputFormat.JAVA_JUNIT_5), solution)

        val lines = Lines(OutputFormat.JAVA_JUNIT_5)
        writer.addExtraInitStatement(lines)
        val init = lines.toString()

        assertEquals(1, init.split("getAsyncApiServerAddress").size - 1, init)

        assertEquals(
            "asyncApiServer_kafka = controller.getAsyncApiServerAddress(\"kafka\");\n" +
                    "if (asyncApiServer_kafka == null) {\n" +
                    "    asyncApiServer_kafka = \"localhost:9092\";\n" +
                    "}",
            init.trimEnd()
        )
    }

    @Test
    fun testTheCallIsWrittenWholeAndCompiles() {

        /*
            The one line every generated test runs on. Asserted whole rather than by substrings:
            the arguments are all strings, so swapping the topic it publishes to with the one it
            waits on would leave every substring assertion passing and every generated test
            waiting on the wrong destination.
         */
        //the JUnit 4 forms differ only in the annotation the shared writer puts above the method
        OutputFormat.values().filter { it.isJavaOrKotlin() && it.isJUnit5() }.forEach { format ->

            startWith(VARIANTS, "--outputFormat=${format.name}") { FakeAsyncApiDriver.replied("""{"answer": 2}""") }

            val (members, body) = membersAndBodyOf(evaluate("ask"))

            val call = "${KafkaTestClientEmitter.HELPER_NAME}(asyncApiServer_broker, \"ask.request\"," +
                    " \"ask.reply\", \"{\\\"value\\\":"

            assertTrue(body.contains(call), "for $format:\n$body")

            //the header the id rides in is the document's, not whatever the default happens to be
            assertTrue(body.contains("\"x-corr-id\""), "for $format:\n$body")

            //the deadline, without which a test polls once and finds nothing
            assertTrue(body.contains("5000L"), "for $format:\n$body")

            //and the message's own headers, flattened onto the end of the call
            assertTrue(body.contains("\"tenant\", \"acme\""), "for $format:\n$body")

            compiles(members, body, format, "AsyncApiCall${format.name}")
        }
    }

    @Test
    fun testAMessageWithNoReplyPassesNothingWhereTheReplyWouldGo() {

        /*
            A fire-and-forget operation: no reply destination, so no deadline and nothing to
            correlate. Each language spells an absent argument its own way, and Python spelling
            it "null" would parse and then fail with a NameError, which is how three of these
            have gone wrong before.
         */
        val expected = mapOf(
            OutputFormat.KOTLIN_JUNIT_5 to "\"tell.request\", null, ",
            OutputFormat.JAVA_JUNIT_5 to "\"tell.request\", null, ",
            OutputFormat.PYTHON_UNITTEST to "\"tell.request\", None, "
        )

        expected.forEach { (format, fragment) ->

            val blackBox = if (format.isPython()) arrayOf("--blackBox=true") else arrayOf()
            startWith(VARIANTS, "--outputFormat=${format.name}", *blackBox) { FakeAsyncApiDriver.fireAndForget() }

            val (members, body) = membersAndBodyOf(evaluate("tell"))

            assertTrue(body.contains(fragment), "for $format:\n$body")
            //nothing came back, so nothing is asserted on
            assertFalse(body.contains("assertNotNull"), "for $format:\n$body")

            if (format.isJavaOrKotlin()) {
                compiles(members, body, format, "AsyncApiNoReply${format.name}")
            }
        }
    }

    @Test
    fun testWhatOneSuiteFoundIsNotLeftBehindForTheNext() {

        /*
            One writer writes every suite of a run, and a run splits its tests into several. A
            suite with nothing to publish over Kafka must come back with no members at all: if
            the servers the last one found were still there, it would assign to a field it never
            declared, and the suite would not compile.
         */
        start { FakeAsyncApiDriver.replied(DOUBLE_RESULT) }

        val kafka = membersOf(evaluate("bessj"))
        assertTrue(kafka.contains(KafkaTestClientEmitter.SERVER_VARIABLE_PREFIX), kafka)

        //the second suite publishes through lines the driver rendered, so nothing is written
        start { FakeAsyncApiDriver.replied(DOUBLE_RESULT).apply { testScript = listOf("publishSomehow()") } }

        val driverRendered = membersOf(evaluate("bessj"))

        assertFalse(driverRendered.contains(KafkaTestClientEmitter.SERVER_VARIABLE_PREFIX), driverRendered)
        assertFalse(driverRendered.contains("KafkaProducer"), driverRendered)

        //and the init statements follow the members, so there is nothing to assign either
        val writer = injector.getInstance(TestCaseWriter::class.java)
        val init = Lines(OutputFormat.KOTLIN_JUNIT_5)
        writer.addExtraInitStatement(init)
        assertEquals("", init.toString().trim())
    }

    @Test
    fun testTwoServersThatWouldShareOneVariableAreNotDeclaredTwice() {

        /*
            A document may name its servers anything; an identifier may not. Two names that
            differ only where a variable cannot would be declared twice under one name, and the
            suite would not compile. The first keeps the variable, and the rest publish to the
            address the document gave them.
         */
        val collidingServers = """
            asyncapi: 3.0.0
            info:
              title: Colliding
              version: 1.0.0
            servers:
              a-b:
                host: one.local:9092
                protocol: kafka
              a.b:
                host: two.local:9092
                protocol: kafka
            channels:
              first:
                address: first.request
                servers:
                  - ${'$'}ref: '#/servers/a-b'
                messages:
                  firstRequest:
                    payload:
                      type: object
                      properties:
                        value:
                          type: integer
              second:
                address: second.request
                servers:
                  - ${'$'}ref: '#/servers/a.b'
                messages:
                  secondRequest:
                    payload:
                      type: object
                      properties:
                        value:
                          type: integer
            operations:
              askFirst:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/first'
              askSecond:
                action: receive
                channel:
                  ${'$'}ref: '#/channels/second'
        """.trimIndent()

        startWith(collidingServers) { FakeAsyncApiDriver.fireAndForget() }

        val members = membersOf(evaluate("askFirst", "askSecond"))

        //one declaration under that name, not two
        assertEquals(1, members.split("asyncApiServer_a_b").size - 1, members)

        //and the one that lost it publishes to the address the document gave it
        val body = bodyOf(evaluate("askFirst", "askSecond"))
        assertTrue(body.contains("asyncApiServer_a_b"), body)
        assertTrue(body.contains("\"two.local:9092\""), body)
    }

    @Test
    fun testAFormatNoTestCanBeWrittenInIsRefusedWhenTheDriverNamesIt() {

        /*
            For this problem type the format is ordinarily left to the driver, in black-box mode
            as well, so it arrives after every option has already been checked. A driver naming
            a language no client is written for has to be refused there rather than reaching the
            writer, which would otherwise emit a call with no client and assertions around it.
         */
        driver = FakeAsyncApiDriver(
            AsyncApiTestInjector.sutInfo(
                AsyncApiAccess.readFromResource(AsyncApiTestInjector.NCS),
                SutInfoDto.OutputFormat.JS_JEST
            )
        ) { FakeAsyncApiDriver.replied(DOUBLE_RESULT) }

        /*
            The sampler is what asks the driver, and it is initialised as the injector is built,
            so the refusal surfaces from there rather than from a later lookup.
         */
        val error = assertThrows(Throwable::class.java) {
            AsyncApiTestInjector.create(driver, "--blackBox=false", "--createTests=true")
                .getInstance(AsyncApiSampler::class.java)
        }

        val reasons = generateSequence(error as Throwable?) { it.cause }.mapNotNull { it.message }.toList()

        assertTrue(
            reasons.any { it.contains("outputFormat") },
            "the format the driver named was not refused: $reasons"
        )
    }

    @Test
    fun testASuiteIsSplitByWhetherPublishingWentWell() {

        /*
            Which file a test lands in, which is what a run with --testSuiteSplitType shows the
            user. There is no status code to read here, so the outcome is what says whether the
            message went out and was answered.
         */
        start("--useExperimentalOracles=true") { FakeAsyncApiDriver.replied(DOUBLE_RESULT) }
        val answered = evaluate("bessj")

        start("--useExperimentalOracles=true") { FakeAsyncApiDriver.silence() }
        val unanswered = evaluate("bessj")

        val config = injector.getInstance(EMConfig::class.java)
        config.testSuiteSplitType = EMConfig.TestSuiteSplitType.FAULTS

        val solution = Solution(
            mutableListOf(answered, unanswered), "Prefix", "Suffix", Termination.NONE, listOf(), listOf()
        )

        val split = TestSuiteSplitter.split(solution, config)
        val byIndividual = split.splitOutcome.flatMap { s -> s.individuals.map { it to s.termination } }

        //the one that was answered is a success; the one that was not is not
        assertEquals(Termination.SUCCESSES, byIndividual.first { it.first == answered }.second)
        assertNotEquals(Termination.SUCCESSES, byIndividual.first { it.first == unanswered }.second)
    }

    @Test
    fun testTheKafkaHelperIsLeftOutWhenNothingCallsIt() {

        //a suite that never publishes over Kafka must not be made to carry the dependency
        start { FakeAsyncApiDriver.replied(DOUBLE_RESULT).apply { testScript = listOf("publishSomehow()") } }

        val members = membersOf(evaluate("bessj"))

        assertFalse(members.contains("KafkaProducer"), members)
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
