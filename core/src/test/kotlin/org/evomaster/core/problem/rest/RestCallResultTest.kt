package org.evomaster.core.problem.rest

import org.evomaster.core.problem.rest.data.RestCallResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import javax.ws.rs.core.MediaType

internal class RestCallResultTest {

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
}
