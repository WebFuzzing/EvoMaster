package org.evomaster.client.java.distance.heuristics;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.evomaster.client.java.distance.heuristics.TruthnessUtils.normalizeValue;
import static org.junit.jupiter.api.Assertions.*;

class TruthnessUtilsTest {

    private static final double DELTA = 0.000001d;

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

    @Test
    public void testGetEqualityTruthnessBooleans() {
        Truthness equal = TruthnessUtils.getEqualityTruthness(true, true);
        Truthness unequal = TruthnessUtils.getEqualityTruthness(true, false);

        assertTrue(equal.isTrue());
        assertSame(TruthnessUtils.TRUE_TRUTHNESS, equal);

        assertTrue(unequal.isFalse());
        assertSame(TruthnessUtils.FALSE_TRUTHNESS, unequal);
    }

    /**
     * Verifies that byte-array equality preserves a gradient based on positional byte mismatches.
     */
    @Test
    public void testGetEqualityTruthnessByteArrays() {
        byte[] messiPhoto = new byte[]{1, 2, 3};
        byte[] samePhoto = new byte[]{1, 2, 3};
        byte[] nearPhoto = new byte[]{1, 2, 4};
        byte[] farPhoto = new byte[]{9, 8, 7};
        byte[] croppedPhoto = new byte[]{1, 2};

        Truthness equal = TruthnessUtils.getEqualityTruthness(messiPhoto, samePhoto);
        Truthness near = TruthnessUtils.getEqualityTruthness(messiPhoto, nearPhoto);
        Truthness far = TruthnessUtils.getEqualityTruthness(messiPhoto, farPhoto);
        Truthness cropped = TruthnessUtils.getEqualityTruthness(messiPhoto, croppedPhoto);

        assertTrue(equal.isTrue());
        assertSame(TruthnessUtils.TRUE_TRUTHNESS, equal);

        assertTrue(near.isFalse());
        assertEquals(0.55d, near.getOfTrue(), DELTA);
        assertEquals(1.0d, near.getOfFalse(), DELTA);

        assertTrue(far.isFalse());
        assertEquals(0.325d, far.getOfTrue(), DELTA);
        assertEquals(1.0d, far.getOfFalse(), DELTA);

        assertTrue(cropped.isFalse());
        assertEquals(0.55d, cropped.getOfTrue(), DELTA);
        assertEquals(1.0d, cropped.getOfFalse(), DELTA);
        assertTrue(near.getOfTrue() > far.getOfTrue());

        assertThrows(NullPointerException.class,
                () -> TruthnessUtils.getEqualityTruthness(null, messiPhoto));
        assertThrows(NullPointerException.class,
                () -> TruthnessUtils.getEqualityTruthness(messiPhoto, null));
    }
}
