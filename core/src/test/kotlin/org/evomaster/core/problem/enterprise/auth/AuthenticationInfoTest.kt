package org.evomaster.core.problem.enterprise.auth

import org.evomaster.core.problem.httpws.auth.AuthenticationHeader
import org.evomaster.core.problem.httpws.auth.HttpWsAuthenticationInfo
import org.evomaster.core.problem.httpws.auth.HttpWsNoAuth
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthenticationInfoTest {

    @Test
    fun testNoAuth(){

        assertTrue(HttpWsNoAuth().isNoAuth())

        val auth = HttpWsAuthenticationInfo("foo", listOf(AuthenticationHeader("foo", "bar")), null, false, null)
        assertFalse(auth.isNoAuth())
    }
}