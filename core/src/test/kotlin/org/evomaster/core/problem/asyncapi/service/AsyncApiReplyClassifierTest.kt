package org.evomaster.core.problem.asyncapi.service

import com.webfuzzing.asyncapi.access.AsyncApiAccess
import com.webfuzzing.asyncapi.models.AsyncApiDocument
import com.webfuzzing.asyncapi.models.AsyncApiMessage
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class AsyncApiReplyClassifierTest {

    private val ncs: AsyncApiDocument = AsyncApiAccess.getAsyncApiFromResource("/asyncapi/sut/ncs-kafka.yaml")

    private val socket: AsyncApiDocument =
        AsyncApiAccess.getAsyncApiFromResource("/asyncapi/artificial/websocket-reply.yaml")

    /**
     * Replies shaped by the parts of JSON Schema the corpus documents happen not to use.
     */
    private val shapes: AsyncApiDocument = AsyncApiAccess.parseFromText(
        """
        asyncapi: 3.0.0
        info:
          title: Shapes
          version: 1.0.0
        components:
          messages:
            pinned:
              payload:
                ${'$'}ref: '#/components/schemas/Pinned'
            tagged:
              payload:
                type: object
                required: [kind]
                properties:
                  kind:
                    enum: [a, b]
            either:
              payload:
                oneOf:
                  - ${'$'}ref: '#/components/schemas/Left'
                  - ${'$'}ref: '#/components/schemas/Right'
            both:
              payload:
                allOf:
                  - ${'$'}ref: '#/components/schemas/Left'
                  - type: object
                    required: [extra]
            scalar:
              payload:
                anyOf:
                  - type: string
                  - type: boolean
            numbers:
              payload:
                type: array
                items:
                  type: integer
            flag:
              payload:
                type: boolean
            exotic:
              payload:
                type: something-this-does-not-know
            part:
              payload:
                ${'$'}ref: '#/components/schemas/Pinned/properties/kind'
          schemas:
            Pinned:
              ${'$'}ref: '#/components/schemas/PinnedTarget'
            PinnedTarget:
              type: object
              required: [kind]
              properties:
                kind:
                  const: pinned
            Left:
              type: object
              required: [left]
              properties:
                left:
                  type: boolean
            Right:
              type: object
              required: [right]
              properties:
                right:
                  type: string
        """.trimIndent()
    )

    private fun classifyAmong(payload: String, vararg candidates: String): String? =
        AsyncApiReplyClassifier.classify(
            payload,
            candidates.map { shapes.messages.getValue(it) },
            shapes.componentSchemas
        )?.id

    private fun repliesOf(document: AsyncApiDocument, operation: String): List<AsyncApiMessage> =
        document.replyMessagesOf(document.operations.getValue(operation))

    private fun classify(document: AsyncApiDocument, operation: String, payload: String?): String? =
        AsyncApiReplyClassifier.classify(payload, repliesOf(document, operation), document.componentSchemas)?.id

    @Test
    fun testAResultIsToldFromAnError() {

        //both reply payloads are behind a $ref to a component schema
        assertEquals("doubleResult", classify(ncs, "bessj", """{"resultAsDouble": 2.5}"""))
        assertEquals("error", classify(ncs, "bessj", """{"error": {"code": 400, "message": "n must be >= 3"}}"""))
    }

    @Test
    fun testAReplyMatchingNoDeclaredMessageIsNotRecognised() {

        assertNull(classify(ncs, "bessj", """{"something": "else"}"""))
        assertNull(classify(ncs, "bessj", """{"error": "not an object"}"""))
        assertNull(classify(ncs, "bessj", """[1, 2, 3]"""))
    }

    @Test
    fun testTextThatIsNotJsonIsNotRecognised() {

        assertNull(classify(ncs, "bessj", "not json at all"))
        assertNull(classify(ncs, "bessj", ""))
        assertNull(classify(ncs, "bessj", null))
    }

    @Test
    fun testAWholeNumberWrittenWithADecimalPointIsStillAnInteger() {

        //JSON Schema counts 3.0 as an integer, and services do write results that way
        assertEquals("intResult", classify(ncs, "checkTriangle", """{"resultAsInt": 3.0}"""))
        assertNull(classify(ncs, "checkTriangle", """{"resultAsInt": 3.5}"""))
    }

    @Test
    fun testTheMostSpecificMatchWinsWhenSeveralCouldFit() {

        /*
            The socket's result messages declare no required fields, so an error payload also
            fits them structurally. The error message requires its "error" field, which makes it
            the more specific description, and so the one chosen.
         */
        assertEquals("error", classify(socket, "recv_list_legs", """{"request_id": "r1", "error": {"code": 404}}"""))
        assertEquals("listLegsResult", classify(socket, "recv_list_legs", """{"request_id": "r1", "legs": ["a", "b"]}"""))
    }

    @Test
    fun testATypeListIsHonoured() {

        //"leg" is declared as [object, "null"]
        assertEquals("getLegResult", classify(socket, "recv_get_leg", """{"request_id": "r1", "leg": null}"""))
        assertEquals("getLegResult", classify(socket, "recv_get_leg", """{"request_id": "r1", "leg": {"id": "x"}}"""))
        assertNull(classify(socket, "recv_get_leg", """{"request_id": "r1", "leg": "not an object"}"""))
    }

    @Test
    fun testWhatItDoesNotUnderstandIsGivenTheBenefitOfTheDoubt() {

        //an array of legs with a non-string inside: items are checked, so this is rejected...
        assertNull(classify(socket, "recv_list_legs", """{"legs": [1, 2]}"""))
        //...but a format, a pattern or a bound is not read at all, so nothing is rejected on their account
        assertEquals("doubleResult", classify(ncs, "bessj", """{"resultAsDouble": -1e308}"""))
    }

    @Test
    fun testAConstDiscriminatorIsReadThroughAChainOfReferences() {

        //the message points at a schema that is itself only a reference to the real one
        assertEquals("pinned", classifyAmong("""{"kind": "pinned"}""", "pinned", "tagged"))
        assertNull(classifyAmong("""{"kind": "other"}""", "pinned", "tagged"))
    }

    @Test
    fun testAnEnumDiscriminatorIsRead() {

        assertEquals("tagged", classifyAmong("""{"kind": "a"}""", "pinned", "tagged"))
        assertEquals("tagged", classifyAmong("""{"kind": "b"}""", "pinned", "tagged"))
        assertNull(classifyAmong("""{"kind": "z"}""", "pinned", "tagged"))
    }

    @Test
    fun testOneOfMatchesEitherBranch() {

        assertEquals("either", classifyAmong("""{"left": true}""", "either"))
        assertEquals("either", classifyAmong("""{"right": "r"}""", "either"))
        assertNull(classifyAmong("""{"neither": 1}""", "either"))
    }

    @Test
    fun testAllOfNeedsEveryBranch() {

        assertEquals("both", classifyAmong("""{"left": true, "extra": 1}""", "both"))
        assertNull(classifyAmong("""{"left": true}""", "both"))
        assertNull(classifyAmong("""{"extra": 1}""", "both"))
    }

    @Test
    fun testAnyOfMatchesAnyBranch() {

        assertEquals("scalar", classifyAmong("\"text\"", "scalar"))
        assertEquals("scalar", classifyAmong("true", "scalar"))
        assertNull(classifyAmong("1", "scalar"))
    }

    @Test
    fun testArrayItemsAreChecked() {

        assertEquals("numbers", classifyAmong("[1, 2, 3]", "numbers"))
        assertEquals("numbers", classifyAmong("[]", "numbers"))
        assertNull(classifyAmong("""[1, "two"]""", "numbers"))
        assertNull(classifyAmong("""{"not": "an array"}""", "numbers"))
    }

    @Test
    fun testABooleanIsNotItsSpelling() {

        assertEquals("flag", classifyAmong("false", "flag"))
        assertNull(classifyAmong("\"false\"", "flag"))
    }

    @Test
    fun testATypeItDoesNotKnowRejectsNothing() {

        assertEquals("exotic", classifyAmong("""{"anything": 1}""", "exotic"))
    }

    @Test
    fun testAReferenceIntoTheMiddleOfASchemaCannotBeJudgedSoItIsNotRejected() {

        /*
            "#/components/schemas/Pinned/properties/kind" names a part of a schema, which is
            legal and which the classifier does not follow. Rejecting on what it cannot read
            would turn every such reply into a false fault, so it matches instead -- and loses
            to anything specific that also matches.
         */
        assertEquals("part", classifyAmong("""{"anything": 1}""", "part"))
        assertEquals("pinned", classifyAmong("""{"kind": "pinned"}""", "part", "pinned"))
    }
}
