package org.evomaster.core.output

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertFalse
import org.evomaster.core.problem.httpws.service.auth.CookieLogin
import org.evomaster.core.problem.rest.ContentType
import org.evomaster.core.problem.rest.HttpVerb
import org.junit.jupiter.api.Test

class CookieLoginGetUrlTest {

    @Test
    fun testCombineUrl() {
        val urlWithFS = CookieLogin("foo","foo","username", "password","/login", HttpVerb.POST, ContentType.JSON)
        assertFalse(urlWithFS.isFullUrlSpecified())
        val expected = "http://localhost:8080/login"
        assertEquals(expected, urlWithFS.getUrl("http://localhost:8080"))
        assertEquals(expected, urlWithFS.getUrl("http://localhost:8080/"))

        val urlWithoutFS = CookieLogin("foo","foo","username", "password","login", HttpVerb.POST, ContentType.JSON)
        assertEquals(expected, urlWithoutFS.getUrl("http://localhost:8080"))
        assertEquals(expected, urlWithoutFS.getUrl("http://localhost:8080/"))

    }

}
