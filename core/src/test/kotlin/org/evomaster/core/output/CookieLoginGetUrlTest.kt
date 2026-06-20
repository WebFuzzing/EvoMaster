package org.evomaster.core.output

import org.evomaster.core.problem.httpws.auth.CallToEndpoint
import org.evomaster.core.problem.httpws.auth.EndpointCallLogin
import org.evomaster.core.problem.rest.data.ContentType
import org.evomaster.core.problem.rest.data.HttpVerb
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CookieLoginGetUrlTest {

    @Test
    fun testCombineUrl() {
        val urlWithFS = EndpointCallLogin("foo", CallToEndpoint("/login",null, "payload", listOf(), HttpVerb.POST, ContentType.JSON))
        val expected = "http://localhost:8080/login"
        assertEquals(expected, urlWithFS.call.getUrl("http://localhost:8080"))
        assertEquals(expected, urlWithFS.call.getUrl("http://localhost:8080/"))
    }

}
