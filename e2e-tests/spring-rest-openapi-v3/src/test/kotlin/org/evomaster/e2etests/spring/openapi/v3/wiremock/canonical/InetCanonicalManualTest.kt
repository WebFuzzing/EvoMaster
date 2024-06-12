package org.evomaster.e2etests.spring.openapi.v3.wiremock.canonical

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.net.InetAddress

class InetCanonicalManualTest {

    @Test
    fun manualTest() {
        // TODO: This is a dummy test for development purpose.
        //  Can be expanded or deleted later if necessary.
        val address = InetAddress.getByName("localhost")

        assertEquals("localhost", address.canonicalHostName)
    }
}
