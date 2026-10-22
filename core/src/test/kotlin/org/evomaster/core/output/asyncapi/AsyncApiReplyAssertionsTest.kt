package org.evomaster.core.output.asyncapi

import org.evomaster.core.EMConfig
import org.evomaster.core.output.Lines
import org.evomaster.core.output.OutputFormat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * What a reply becomes, as assertions, in Java and Kotlin.
 */
class AsyncApiReplyAssertionsTest {

    private val variable = "res_0"

    private val body = "body_0"

    private val unstable = "unstable"

    private fun emit(
        payload: String?,
        format: OutputFormat,
        config: EMConfig = EMConfig(),
        isFieldToSkip: (String) -> Boolean = { it == "id" },
        isSuitableToPrint: (String) -> Boolean = { !it.contains(unstable) }
    ): String {
        val lines = Lines(format)
        AsyncApiReplyAssertions(config, format, isFieldToSkip, isSuitableToPrint)
            .emit(lines, payload, variable, body)
        return lines.toString()
    }

    @Test
    fun testNothingIsSaidWhenAssertionsAreOff() {

        val config = EMConfig().apply { enableBasicAssertions = false }

        OutputFormat.values().filter { it.isJavaOrKotlin() }.forEach {
            assertEquals("", emit("""{"a": 1}""", it, config).trim(), "for $it")
        }
    }

    @Test
    fun testAnEmptyReplyIsAssertedEmpty() {

        assertTrue(emit("", OutputFormat.KOTLIN_JUNIT_5).contains("assertTrue(res_0 == null || res_0.isEmpty())"))
        assertTrue(emit("", OutputFormat.JAVA_JUNIT_5).contains("assertTrue(res_0 == null || res_0.isEmpty());"))
    }

    @Test
    fun testAFieldIsAssertedByName() {

        val payload = """{"name": "abc", "n": 7}"""

        val kotlin = emit(payload, OutputFormat.KOTLIN_JUNIT_5)
        assertTrue(kotlin.contains("""assertTrue(body_0.has("name"))"""), kotlin)
        assertTrue(kotlin.contains("""assertEquals("abc", body_0.get("name").asText())"""), kotlin)
        assertTrue(kotlin.contains("""assertEquals(7, body_0.get("n").asLong())"""), kotlin)
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

        //Jackson reads it as an infinity, which is not a literal in Java nor Kotlin
        val emitted = emit("""{"x": 1e400}""", OutputFormat.KOTLIN_JUNIT_5)

        assertFalse(emitted.contains("Infinity"), emitted)
        assertTrue(emitted.contains("outside the range of a double"), emitted)
    }

    @Test
    fun testADoubleIsComparedWithinATolerance() {

        assertTrue(emit("""{"x": 1.5}""", OutputFormat.KOTLIN_JUNIT_5).contains("0.001"))
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
    fun testAnEmptyCollectionIsAssertedEmpty() {

        //an empty array is its size, which is how a full one is asserted too
        assertTrue(emit("""{"xs": []}""", OutputFormat.KOTLIN_JUNIT_5).contains("""assertEquals(0, body_0.get("xs").size())"""))

        //an empty object is asked whether it is empty, which each language spells its own way
        assertTrue(emit("""{"o": {}}""", OutputFormat.KOTLIN_JUNIT_5).contains("""assertTrue(body_0.get("o").isEmpty())"""))
        assertTrue(emit("""{"o": {}}""", OutputFormat.JAVA_JUNIT_5).contains("""assertTrue(body_0.get("o").isEmpty());"""))
    }

    @Test
    fun testAFieldToSkipHasOnlyItsPresenceAsserted() {

        val emitted = emit("""{"id": 7, "kept": 2}""", OutputFormat.KOTLIN_JUNIT_5)

        assertTrue(emitted.contains("""assertTrue(body_0.has("id"))"""), emitted)
        assertFalse(emitted.contains("""body_0.get("id")"""), emitted)
        assertTrue(emitted.contains("id is not asserted on"), emitted)

        assertTrue(emitted.contains("""assertEquals(2, body_0.get("kept").asLong())"""), emitted)
    }

    @Test
    fun testAValueNotSuitableToPrintBecomesAComment() {

        val emitted = emit("""{"where": "$unstable"}""", OutputFormat.KOTLIN_JUNIT_5)

        assertFalse(emitted.contains("asText()"), emitted)
        assertTrue(emitted.contains("where is not asserted on"), emitted)
    }

    @Test
    fun testAReplyThatIsNotJsonIsAssertedOnAsText() {

        //a plain-text reply is ordinary for a transport that carries no JSON
        assertTrue(emit("just text", OutputFormat.KOTLIN_JUNIT_5)
            .contains("""assertTrue(res_0!!.contains("just text"))"""))
        assertTrue(emit("just text", OutputFormat.JAVA_JUNIT_5)
            .contains("""assertTrue(res_0.contains("just text"));"""))

        assertFalse(emit("just text", OutputFormat.KOTLIN_JUNIT_5).contains("readTree"))
    }

    @Test
    fun testAReplyThatIsNotJsonNorSuitableToPrintBecomesAComment() {

        val emitted = emit("listening on $unstable", OutputFormat.KOTLIN_JUNIT_5)

        assertFalse(emitted.contains("contains("), emitted)
        assertTrue(emitted.contains("the reply is not JSON"), emitted)
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
    }

    @Test
    fun testACollectionLimitOfZeroAssertsOnlyTheSize() {

        /*
            The option's way of asking for quieter tests. The size is still asserted, so a run
            that returned five elements says so; nothing is said about what they were.
         */
        val config = EMConfig().apply { maxAssertionForDataInCollection = 0 }
        val emitted = emit("""{"xs": [1, 2, 3]}""", OutputFormat.KOTLIN_JUNIT_5, config)

        assertTrue(emitted.contains("""assertEquals(3, body_0.get("xs").size())"""), emitted)
        assertFalse(emitted.contains("get(0)"), emitted)
        assertTrue(emitted.contains("the remaining 3 elements are not asserted on"), emitted)
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
    fun testABooleanAndANullAreAsserted() {

        val payload = """{"ok": true, "none": null}"""

        assertTrue(emit(payload, OutputFormat.KOTLIN_JUNIT_5).contains("assertTrue(body_0.get(\"none\").isNull())"))
        assertTrue(emit(payload, OutputFormat.JAVA_JUNIT_5).contains("assertTrue(body_0.get(\"none\").isNull());"))
        assertTrue(emit(payload, OutputFormat.KOTLIN_JUNIT_5).contains("assertEquals(true, body_0.get(\"ok\").asBoolean())"))
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

    @Test
    fun testAnUnsupportedFormatIsRejected() {

        assertThrows(IllegalArgumentException::class.java) {
            emit("""{"a": 1}""", OutputFormat.PYTHON_UNITTEST)
        }
    }
}
