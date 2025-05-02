package org.evomaster.core.output

import junit.framework.TestCase.assertEquals
import org.evomaster.core.problem.httpws.auth.EndpointCallLogin
import org.evomaster.core.problem.rest.data.ContentType
import org.evomaster.core.problem.rest.data.HttpVerb
import org.junit.jupiter.api.Test

class CookieLoginGetUrlTest {

    @Test
    fun testCombineUrl() {
        val urlWithFS = EndpointCallLogin("foo","/login",null, "payload", HttpVerb.POST, ContentType.JSON)
        val expected = "http://localhost:8080/login"
        assertEquals(expected, urlWithFS.getUrl("http://localhost:8080"))
        assertEquals(expected, urlWithFS.getUrl("http://localhost:8080/"))
    }

}
