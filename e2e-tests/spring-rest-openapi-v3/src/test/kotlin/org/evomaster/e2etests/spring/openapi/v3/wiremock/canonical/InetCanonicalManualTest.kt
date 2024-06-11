package org.evomaster.e2etests.spring.openapi.v3.wiremock.canonical

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.InetAddress

class InetCanonicalManualTest {

    @Test
    fun manualTest() {
        val address = InetAddress.getByName("localhost")

        val canonical = address.canonicalHostName

        assertEquals("localhost", canonical)
    }
}
