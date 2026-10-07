package org.evomaster.core.output.asyncapi

import org.evomaster.core.EMConfig
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * What a reply becomes, as assertions, in each language a suite can be written in.
 *
 * The three forms are spelled out rather than shared, because the point of this is to catch the
 * one that was forgotten: a Python suite is only ever parsed, never run, by the tests here.
 */
class AsyncApiReplyAssertionsTest {

    private val variable = "res_0"

    private val body = "body_0"

    private fun emit(payload: String?, format: OutputFormat, config: EMConfig = EMConfig()): String {
        val lines = Lines(format)
        AsyncApiReplyAssertions.emit(lines, payload, variable, body, config, format)
        return lines.toString()
    }

    @Test
    fun testNothingIsSaidWhenAssertionsAreOff() {

        val config = EMConfig().apply { enableBasicAssertions = false }

        OutputFormat.values().filter { it.isJavaOrKotlin() || it.isPython() }.forEach {
            assertEquals("", emit("""{"a": 1}""", it, config).trim(), "for $it")
        }
    }

    @Test
    fun testAnEmptyReplyIsAssertedInTheLanguageOfTheSuite() {

        /*
            The case that was wrong: Python was handed Kotlin's isNullOrEmpty, which parses and
            then fails at run time with an AttributeError.
         */
        assertTrue(emit("", OutputFormat.KOTLIN_JUNIT_5).contains("assertTrue(res_0.isNullOrEmpty())"))
        assertTrue(emit("", OutputFormat.JAVA_JUNIT_5).contains("assertTrue(res_0 == null || res_0.isEmpty());"))

        val python = emit("", OutputFormat.PYTHON_UNITTEST)
        assertTrue(python.contains("assert res_0 is None or len(res_0) == 0"), python)
        assertFalse(python.contains("isNullOrEmpty"), python)
    }

    @Test
    fun testAFieldIsReadTheWayItsLanguageReadsOne() {

        val payload = """{"name": "abc", "n": 7}"""

        val kotlin = emit(payload, OutputFormat.KOTLIN_JUNIT_5)
        assertTrue(kotlin.contains("""assertTrue(body_0.has("name"))"""), kotlin)
        assertTrue(kotlin.contains("""assertEquals("abc", body_0.get("name").asText())"""), kotlin)
        assertTrue(kotlin.contains("""assertEquals(7, body_0.get("n").asLong())"""), kotlin)

        val python = emit(payload, OutputFormat.PYTHON_UNITTEST)
        assertTrue(python.contains("""body_0 = json.loads(res_0)"""), python)
        assertTrue(python.contains(""""name" in body_0"""), python)
        assertTrue(python.contains("""body_0["name"] == "abc""""), python)
        //a dict is not a tree of nodes, so nothing asks it for a typed accessor
        assertFalse(python.contains("asText()"), python)
    }

    @Test
    fun testAFieldNamedLikeATemplateDoesNotBreakTheSuite() {

        /*
            '$ref' is an ordinary JSON field name, and Kotlin would read it as the start of a
            template unless it is escaped where it is written.
         */
        val kotlin = emit("""{"${'$'}ref": "x"}""", OutputFormat.KOTLIN_JUNIT_5)

        assertTrue(kotlin.contains("""has("\${'$'}ref")"""), kotlin)
        assertFalse(kotlin.contains("""has("${'$'}ref")"""), kotlin)
    }

    @Test
    fun testANumberOutsideADoubleIsNotAssertedOn() {

        //Jackson reads it as an infinity, which is not a literal in any of the three languages
        val emitted = emit("""{"x": 1e400}""", OutputFormat.KOTLIN_JUNIT_5)

        assertFalse(emitted.contains("Infinity"), emitted)
        assertTrue(emitted.contains("outside the range of a double"), emitted)
    }

    @Test
    fun testADoubleIsComparedWithinATolerance() {

        assertTrue(emit("""{"x": 1.5}""", OutputFormat.KOTLIN_JUNIT_5).contains("0.001"))
        assertTrue(emit("""{"x": 1.5}""", OutputFormat.PYTHON_UNITTEST).contains("abs(body_0[\"x\"] - 1.5) < 0.001"))
    }

    @Test
    fun testOnlyTheFirstFewElementsOfACollectionAreAsserted() {

        val config = EMConfig().apply { maxAssertionForDataInCollection = 2 }
        val emitted = emit("""{"xs": [1, 2, 3, 4]}""", OutputFormat.KOTLIN_JUNIT_5, config)

        assertTrue(emitted.contains("body_0.get(\"xs\").size()"), emitted)
        assertTrue(emitted.contains("get(0)"), emitted)
        assertTrue(emitted.contains("get(1)"), emitted)
        assertFalse(emitted.contains("get(2)"), emitted)
    }

    @Test
    fun testAnEmptyCollectionIsAssertedEmptyInEachLanguage() {

        //an empty array is its size, which is how a full one is asserted too
        assertTrue(emit("""{"xs": []}""", OutputFormat.KOTLIN_JUNIT_5).contains("""assertEquals(0, body_0.get("xs").size())"""))
        assertTrue(emit("""{"xs": []}""", OutputFormat.PYTHON_UNITTEST).contains("""assert len(body_0["xs"]) == 0"""))

        //an empty object is asked whether it is empty, which each language spells its own way
        assertTrue(emit("""{"o": {}}""", OutputFormat.KOTLIN_JUNIT_5).contains("""assertTrue(body_0.get("o").isEmpty)"""))
        assertTrue(emit("""{"o": {}}""", OutputFormat.JAVA_JUNIT_5).contains("""assertTrue(body_0.get("o").isEmpty());"""))
        assertTrue(emit("""{"o": {}}""", OutputFormat.PYTHON_UNITTEST).contains("""assert len(body_0["o"]) == 0"""))
    }

    @Test
    fun testTheFieldsThatChangeBetweenRunsAreSkipped() {

        val emitted = emit("""{"id": 7, "timestamp": 1, "self": "x", "kept": 2}""", OutputFormat.KOTLIN_JUNIT_5)

        /*
            That the field is there is stable and is asserted; what it holds is not, so only the
            value is left alone. This is what the REST writer does with the same three names.
         */
        assertTrue(emitted.contains("""assertTrue(body_0.has("id"))"""), emitted)
        listOf("id", "timestamp", "self").forEach {
            assertFalse(emitted.contains("""body_0.get("$it")"""), emitted)
            assertTrue(emitted.contains("$it is not asserted on"), emitted)
        }

        assertTrue(emitted.contains("""assertEquals(2, body_0.get("kept").asLong())"""), emitted)
    }

    @Test
    fun testAValueThatIsNotStableBetweenRunsBecomesAComment() {

        //an address with a port is the REST writer's own example of what not to assert on
        val emitted = emit("""{"where": "localhost:12345"}""", OutputFormat.KOTLIN_JUNIT_5)

        assertFalse(emitted.contains("localhost:12345"), emitted)
        assertTrue(emitted.contains("not stable between runs"), emitted)
    }

    @Test
    fun testAReplyThatIsNotJsonIsAssertedOnAsTextInEveryLanguage() {

        //a plain-text reply is ordinary for a transport that carries no JSON
        assertTrue(emit("just text", OutputFormat.KOTLIN_JUNIT_5)
            .contains("""assertTrue(res_0!!.contains("just text"))"""))
        assertTrue(emit("just text", OutputFormat.JAVA_JUNIT_5)
            .contains("""assertTrue(res_0.contains("just text"));"""))
        assertTrue(emit("just text", OutputFormat.PYTHON_UNITTEST)
            .contains("""assert "just text" in res_0"""))

        assertFalse(emit("just text", OutputFormat.KOTLIN_JUNIT_5).contains("readTree"))
    }

    @Test
    fun testAReplyThatCannotBePrintedSaysSoRatherThanAssertingNothing() {

        //an address with a port is the REST writer's own example of what is not stable
        val emitted = emit("listening on localhost:12345", OutputFormat.KOTLIN_JUNIT_5)

        assertFalse(emitted.contains("localhost:12345"), emitted)
        assertTrue(emitted.contains("not asserted on"), emitted)
    }

    @Test
    fun testANumberTooLargeForAnIntCarriesJavasSuffix() {

        /*
            Java picks assertEquals(long, long) by widening within an int's range, but a literal
            outside it is not an int at all and does not compile without the suffix.
         */
        val big = """{"n": 5000000000}"""

        assertTrue(emit(big, OutputFormat.JAVA_JUNIT_5).contains("assertEquals(5000000000L,"), emit(big, OutputFormat.JAVA_JUNIT_5))
        //Kotlin types the literal itself, so a suffix there would be noise
        assertTrue(emit(big, OutputFormat.KOTLIN_JUNIT_5).contains("assertEquals(5000000000,"), emit(big, OutputFormat.KOTLIN_JUNIT_5))
        assertTrue(emit(big, OutputFormat.PYTHON_UNITTEST).contains("== 5000000000"), emit(big, OutputFormat.PYTHON_UNITTEST))
    }

    @Test
    fun testANegativeCollectionLimitAssertsTheWholeCollection() {

        //the option's way of saying "no limit", which must not truncate to nothing
        val config = EMConfig().apply { maxAssertionForDataInCollection = -1 }
        val emitted = emit("""{"xs": [1, 2, 3]}""", OutputFormat.KOTLIN_JUNIT_5, config)

        assertTrue(emitted.contains("get(0)"), emitted)
        assertTrue(emitted.contains("get(1)"), emitted)
        assertTrue(emitted.contains("get(2)"), emitted)
    }

    @Test
    fun testABooleanAndANullReadAsTheirOwnLanguageWritesThem() {

        val payload = """{"ok": true, "none": null}"""

        assertTrue(emit(payload, OutputFormat.KOTLIN_JUNIT_5).contains("assertTrue(body_0.get(\"none\").isNull)"))
        assertTrue(emit(payload, OutputFormat.JAVA_JUNIT_5).contains("assertTrue(body_0.get(\"none\").isNull());"))

        val python = emit(payload, OutputFormat.PYTHON_UNITTEST)
        assertTrue(python.contains("body_0[\"none\"] is None"), python)
        //Python writes it True, not true
        assertTrue(python.contains("== True"), python)
    }

    @Test
    fun testAReplyLargerThanTheThresholdIsNotAssertedOn() {

        val config = EMConfig().apply { maxResponseByteSize = 10 }
        val emitted = emit("""{"x": "aaaaaaaaaaaaaaaaaaaaaaaaa"}""", OutputFormat.KOTLIN_JUNIT_5, config)

        assertTrue(emitted.contains("larger than the threshold"), emitted)
        assertFalse(emitted.contains("assertTrue"), emitted)
        assertFalse(emitted.contains("assertEquals"), emitted)
        assertFalse(emitted.contains("readTree"), emitted)
    }
}
