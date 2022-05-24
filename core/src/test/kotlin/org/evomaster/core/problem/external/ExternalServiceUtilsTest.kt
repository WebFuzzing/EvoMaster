package org.evomaster.core.problem.external

import org.evomaster.core.problem.external.service.ExternalServiceUtils.isAddressAvailable
import org.evomaster.core.problem.external.service.ExternalServiceUtils.nextIPAddress
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.InetSocketAddress
import java.net.ServerSocket


class ExternalServiceUtilsTest {

    /**
     * Tests the next IP address generator. If IP address is out of range from
     * loopback range should throw exception as well as if IP address is given
     * in wrong format.
     */
    @Test
    fun testNextAddress() {
        assertEquals("127.1.100.2", nextIPAddress("127.1.100.1"))
        assertEquals("127.10.10.3", nextIPAddress("127.10.10.2"))
        assertNotEquals("127.0.0.1", nextIPAddress("127.0.0.0"))
        assertNotEquals("127.30.255.1", nextIPAddress("127.30.254.255"))

        assertThrows(Exception::class.java) {
            nextIPAddress("127.255.255.254")
        }
        assertThrows(IllegalArgumentException::class.java) {
            nextIPAddress("10.0.0")
        }
        assertThrows(Exception::class.java) {
            nextIPAddress("10.0.0.1")
        }
    }

    /**
     * Test to check IP and port availability checker. Test should assert false
     * when ServerSocket is open in the given port. Same should assert true
     * once socket is closed.
     */
    @Test
    fun testIPAddressAvailability() {
        val s = ServerSocket()
        s.bind(InetSocketAddress("127.0.0.2", 12302))

        assertFalse(isAddressAvailable("127.0.0.2", 12302))
        assertTrue(isAddressAvailable("127.0.0.2", 12301))
        s.close()
        assertTrue(isAddressAvailable("127.0.0.2", 12302))
    }
}