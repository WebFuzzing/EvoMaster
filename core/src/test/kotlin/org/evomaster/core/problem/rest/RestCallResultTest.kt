package org.evomaster.core.problem.rest

import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test
import javax.ws.rs.core.MediaType

internal class RestCallResultTest {

    @Test
    fun givenAStringIdWhenGetResourceIdThenItIsReturnedAsString() {
        val rc = RestCallResult("", false)
        rc.setBody("{\"id\":\"735\"}")
        rc.setBodyType(MediaType.APPLICATION_JSON_TYPE)

        val res = rc.getResourceId()

        assertEquals("735", res)
    }
    @Test
    fun givenANumericIdWhenGetResourceIdThenItIsReturnedAsString() {
        val rc = RestCallResult("",false)
        rc.setBody("{\"id\":735}")
        rc.setBodyType(MediaType.APPLICATION_JSON_TYPE)

        val res = rc.getResourceId()

        assertEquals("735", res)
    }
}
