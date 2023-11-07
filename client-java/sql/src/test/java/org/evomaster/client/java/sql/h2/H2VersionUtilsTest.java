package org.evomaster.client.java.sql.h2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class H2VersionUtilsTest {

    @Test
    public void testEqual() {
        assertTrue(H2VersionUtils.isVersionGreaterOrEqual("1.4.193","1.4.193"));
    }

    @Test
    public void testLess() {
        assertTrue(H2VersionUtils.isVersionGreaterOrEqual("1.4.200","1.4.193"));
    }

    @Test
    public void testLessVersion2() {
        assertTrue(H2VersionUtils.isVersionGreaterOrEqual("2.1.214","1.3.193"));
    }

    @Test
    public void testGreater() {
        assertFalse(H2VersionUtils.isVersionGreaterOrEqual("1.4.193","1.4.199"));
    }

}
