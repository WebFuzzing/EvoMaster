package org.evomaster.core.problem.rest

import org.evomaster.core.problem.rest.data.RestCallResult
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
        val body = createCallResult(same)
        val same = createCallResult(same)
        val diff = createCallResult(diff)

        body.setFlakiness(same)
        assertNotNull(body.getBodyType())

        assertNull(body.getFlakyBody())
        assertNull(body.getFlakyBodyType())

        body.setFlakiness(diff)
        assertEquals(diff.getBody(), body.getFlakyBody())
        assertNull(body.getFlakyBodyType())

    }


    @Test
    fun testSetFlakiness(){
        val code = 201
        val diffcode = 500
        val msg = "hello"
        val diffmsg = "hello!"

        val body = RestCallResult("1")
        val same = RestCallResult("2")
        val diff = RestCallResult("3")

        body.setStatusCode(code)
        same.setStatusCode(code)
        diff.setStatusCode(diffcode)

        body.setErrorMessage(msg)
        same.setErrorMessage(msg)
        diff.setErrorMessage(diffmsg)

        body.setFlakiness(same)
        assertNull(body.getFlakyStatusCode())
        assertNull(body.getFlakyErrorMessage())

        body.setFlakiness(diff)
        assertEquals(diffcode, body.getFlakyStatusCode())
        assertEquals(diffmsg, body.getFlakyErrorMessage())

    }
}
