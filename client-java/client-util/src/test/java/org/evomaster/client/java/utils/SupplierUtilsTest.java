package org.evomaster.client.java.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class SupplierUtilsTest {

    @Test
    public void testTheSuppliedValueIsReturned() {
        assertEquals("foo", SupplierUtils.getOrNullOnFailure(() -> "foo", "warning"));
    }

    @Test
    public void testASuppliedNullIsReturned() {
        assertNull(SupplierUtils.<String>getOrNullOnFailure(() -> null, "warning"));
    }

    @Test
    public void testAFailureGivesNull() {
        assertNull(SupplierUtils.<String>getOrNullOnFailure(() -> {
            throw new IllegalStateException("broken");
        }, "warning"));
    }
}
