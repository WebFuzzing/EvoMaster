package org.evomaster.client.java.distance.heuristics;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.evomaster.client.java.distance.heuristics.TruthnessUtils.normalizeValue;
import static org.junit.jupiter.api.Assertions.*;

class TruthnessUtilsTest {

    @Test
    public void testGetEqualityTruthnessEqualsUUID() {
        UUID left = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID right = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        Truthness t = TruthnessUtils.getEqualityTruthness(left, right);
        assertTrue(t.isTrue());
        assertEquals(1.0, t.getOfTrue());
        assertEquals(0.0, t.getOfFalse());
    }

    @Test
    public void testGetEqualityTruthnessNotEqualsUUID() {
        UUID left = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID right = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
        Truthness t = TruthnessUtils.getEqualityTruthness(left, right);
        assertFalse(t.isTrue());
        assertEquals(1.0 - normalizeValue(1), t.getOfTrue());
        assertEquals(1.0, t.getOfFalse());
    }

}
