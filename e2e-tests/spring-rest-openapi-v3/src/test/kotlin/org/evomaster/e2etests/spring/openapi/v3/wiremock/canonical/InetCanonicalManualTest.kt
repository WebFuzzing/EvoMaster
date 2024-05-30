package org.evomaster.e2etests.spring.openapi.v3.wiremock.canonical

import org.evomaster.e2etests.spring.openapi.v3.SpringTestBase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.InetAddress
import java.net.InetSocketAddress

class InetCanonicalManualTest {



    @Test
    fun manualTest() {
        val address = InetAddress.getByName("127.0.0.1")

//        val canonical = InetAddress.getByName(address.address.toString())

        assertEquals("localhost", address.canonicalHostName)
    }
}
