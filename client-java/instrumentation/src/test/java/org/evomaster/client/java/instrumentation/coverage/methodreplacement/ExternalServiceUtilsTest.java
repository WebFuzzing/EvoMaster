package org.evomaster.client.java.instrumentation.coverage.methodreplacement;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.ExternalServiceUtils.skipHostnameOrIp;
import static org.junit.jupiter.api.Assertions.*;

class ExternalServiceUtilsTest {

    @Test
    public void testCanonical() {

        InetAddress address = InetAddress.getLoopbackAddress();
        String canonical = address.getCanonicalHostName();

        assertTrue(skipHostnameOrIp(canonical));
    }
}