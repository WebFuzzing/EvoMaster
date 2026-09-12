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
}
