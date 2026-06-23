package org.evomaster.core.problem.rest

import org.evomaster.core.problem.rest.data.RestCallResult
import org.evomaster.core.problem.httpws.HttpWsCallResult.FlakyObservationSource
import org.evomaster.core.problem.httpws.HttpWsCallResult.ResponseField
import org.evomaster.core.utils.FlakinessInferenceUtil.UUID_MARKER
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import javax.ws.rs.core.MediaType

internal class RestCallResultTest {

    companion object{
        @JvmStatic
        fun getFlakyBodyDataProvider(): Stream<Arguments> {

            return Stream.of(
                Arguments.of("42", "-1"),
                Arguments.of("{\"id\":\"42\"}", "{\"id\":\"735\"}"),
                Arguments.of("""
            {
                "solid": "a",
                "timid": "b",
                "fooId": "c",
                "void":  "d"
            }
        """, """
            {
                "solid": "a",
                "timid": "b",
                "fooId": "d",
                "void":  "d"
            }
        """),
            )

        }
    }

    @Test
    fun givenAStringIdWhenGetResourceIdThenItIsReturnedAsString() {
        val rc = RestCallResult("", false)
        rc.setBody("{\"id\":\"735\"}")
        rc.setBodyType(MediaType.APPLICATION_JSON_TYPE)

        val res = rc.getResourceId()!!.value

        assertEquals("735", res)
    }
    @Test
    fun givenANumericIdWhenGetResourceIdThenItIsReturnedAsString() {
        val rc = RestCallResult("",false)
        rc.setBody("{\"id\":735}")
        rc.setBodyType(MediaType.APPLICATION_JSON_TYPE)

        val res = rc.getResourceId()!!.value

        assertEquals("735", res)
    }

    private fun createCallResult(body: String) : RestCallResult {
        val rc = RestCallResult("",false)
        rc.setBody(body)
        rc.setBodyType(MediaType.APPLICATION_JSON_TYPE)
        return rc
    }

    @Test
    fun testDisambiguation(){
        val rc = createCallResult("""
            {
                "solid": "a",
                "timid": "b",
                "fooId": "c",
                "void":  "d"
            }
        """.trimIndent())

        val res = rc.getResourceId()!!

        assertEquals("c", res.value)
        assertEquals("/fooId", res.pointer)
    }

    @Test
    fun testWrappedData(){
        val rc = createCallResult("""
            {
                "errors": null,
                "data": {
                    "foo": "a",
                    "placid": "b",
                    "bar_id": "c"
                }
            }
        """.trimIndent())

        val res = rc.getResourceId()!!

        assertEquals("c", res.value)
        assertEquals("/data/bar_id", res.pointer)
    }

    @Test
    fun testWrongWrapping(){
        val rc = createCallResult("""
            {
                "data": null
                "errors": {                
                    "id": "foo",
                    "message": "an error",
                }
            }
        """.trimIndent())

        val res = rc.getResourceId()
        assertNull(res)
    }

    @Test
    fun testPrimitive(){
        val rc = createCallResult("42")

        val res = rc.getResourceId()!!

        assertEquals("42", res.value)
        assertEquals("/", res.pointer)
    }

    @ParameterizedTest
    @MethodSource("getFlakyBodyDataProvider")
    fun testSetFlakinessInBody(same: String, diff: String){
        val original = createCallResult(same)
        val sameResult = createCallResult(same)
        val differentResult = createCallResult(diff)

        original.recordFlakyObservation(sameResult, 1)
        assertNotNull(original.getBodyType())

        assertNull(original.getFlakyObservation(1))
        assertNull(original.getFlakyVariation(ResponseField.BODY))
        assertNull(original.getFlakyVariation(ResponseField.BODY_TYPE))

        original.recordFlakyObservation(differentResult, 2)

        val observation = original.getFlakyObservation(2)
        assertNotNull(observation)
        assertEquals(differentResult.getBody(), observation!!.differences[ResponseField.BODY])
        assertEquals(mapOf(2 to differentResult.getBody()), original.getFlakyVariation(ResponseField.BODY)!!.valuesByExecIndex)
        assertNull(original.getFlakyVariation(ResponseField.BODY_TYPE))

    }


    @Test
    fun testSetFlakiness(){
        val code = 201
        val diffcode = 500
        val msg = "hello"
        val diffmsg = "hello!"

        val original = RestCallResult("1")
        val sameResult = RestCallResult("2")
        val differentResult = RestCallResult("3")

        original.setStatusCode(code)
        sameResult.setStatusCode(code)
        differentResult.setStatusCode(diffcode)

        original.setErrorMessage(msg)
        sameResult.setErrorMessage(msg)
        differentResult.setErrorMessage(diffmsg)

        original.recordFlakyObservation(sameResult, 1)
        assertNull(original.getFlakyObservation(1))
        assertNull(original.getFlakyVariation(ResponseField.STATUS_CODE))
        assertNull(original.getFlakyVariation(ResponseField.ERROR_MESSAGE))

        original.recordFlakyObservation(differentResult, 2)

        val observation = original.getFlakyObservation(2)
        assertNotNull(observation)
        assertEquals(diffcode.toString(), observation!!.differences[ResponseField.STATUS_CODE])
        assertEquals(diffmsg, observation.differences[ResponseField.ERROR_MESSAGE])
        assertEquals(mapOf(2 to diffcode.toString()), original.getFlakyVariation(ResponseField.STATUS_CODE)!!.valuesByExecIndex)
        assertEquals(mapOf(2 to diffmsg), original.getFlakyVariation(ResponseField.ERROR_MESSAGE)!!.valuesByExecIndex)

    }

    @Test
    fun testFlakinessIsRecordedAsExecutionDeltas(){
        val original = createCallResult("""{"id":"42"}""")
        original.setStatusCode(200)

        val same = createCallResult("""{"id":"42"}""")
        same.setStatusCode(200)

        val different = createCallResult("""{"id":"735"}""")
        different.setStatusCode(500)

        original.recordFlakyObservation(same, 1)
        original.recordFlakyObservation(different, 2)

        assertNull(original.getFlakyObservation(1))

        val observation = original.getFlakyObservation(2)
        assertNotNull(observation)
        assertEquals("500", observation!!.differences[ResponseField.STATUS_CODE])
        assertEquals("""{"id":"735"}""", observation.differences[ResponseField.BODY])

        val statusVariation = original.getFlakyVariation(ResponseField.STATUS_CODE)
        assertNotNull(statusVariation)
        assertEquals(mapOf(2 to "500"), statusVariation!!.valuesByExecIndex)
    }

    @Test
    fun testStaticFlakinessInferenceIsStoredSeparatelyFromExecutionDeltas(){
        val original = createCallResult("""{"id":"550e8400-e29b-41d4-a716-446655440000"}""")

        original.recordStaticFlakyInference()

        assertNull(original.getFlakyObservation(1))

        val staticObservation = original.getStaticFlakyObservation()
        assertNotNull(staticObservation)
        assertEquals(FlakyObservationSource.STATIC_INFERENCE, staticObservation!!.source)
        assertNull(staticObservation.execIndex)
        assertEquals("""{"id":"$UUID_MARKER"}""", staticObservation.differences[ResponseField.BODY])
    }
}
