package org.evomaster.client.java.instrumentation.shared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IPAddressValidatorTest {

    @Test
    public void testIPV4() {
        assertTrue(IPAddressValidator.isValidInet4Address("192.168.0.1"));
        assertTrue(IPAddressValidator.isValidInet4Address("10.168.0.1"));
        assertTrue(IPAddressValidator.isValidInet4Address("127.168.0.200"));
        assertFalse(IPAddressValidator.isValidInet4Address("192.168.0.256"));
        assertFalse(IPAddressValidator.isValidInet4Address("256.168.0.256"));
        assertFalse(IPAddressValidator.isValidInet4Address("01.168.0.256"));
        assertFalse(IPAddressValidator.isValidInet4Address("192.168.1.256"));
        assertFalse(IPAddressValidator.isValidInet4Address("1.1.1"));
    }

    @Test
    public void testIPV6() {
        assertTrue(IPAddressValidator.isValidInet6Address("2001:db8:3333:4444:5555:6666:7777:8888"));
        assertTrue(IPAddressValidator.isValidInet6Address("2001:db8:3333:4444:cccc:dddd:eeee:ffff"));
        assertTrue(IPAddressValidator.isValidInet6Address("2002:c0a8:101::42"));
        assertTrue(IPAddressValidator.isValidInet6Address("2000::"));
        assertTrue(IPAddressValidator.isValidInet6Address("::1234:5678"));
        assertFalse(IPAddressValidator.isValidInet6Address(":1234:5678"));
        assertFalse(IPAddressValidator.isValidInet6Address("192.168.1.1"));
    }
}
