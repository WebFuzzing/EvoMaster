package org.evomaster.client.java.sql.distance.advanced.helper;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static java.lang.Double.MIN_VALUE;
import static org.evomaster.client.java.sql.distance.advanced.helpers.distance.BooleanDistanceHelper.calculateDistanceForEquals;
import static org.evomaster.client.java.sql.distance.advanced.helpers.distance.BooleanDistanceHelper.calculateDistanceForNotEquals;

public class BooleanDistanceHelperTest {

    @Test
    public void testEquals() { //Equal booleans
        assertEquals(0D, calculateDistanceForEquals(true, true));
    }

    @Test
    public void testEquals2() { //Different booleans
        assertEquals(1D, calculateDistanceForEquals(false, true));
    }

    @Test
    public void testNotEquals() { //Equal booleans
        assertEquals(1D, calculateDistanceForNotEquals(true, true));
    }

    @Test
    public void testNotEquals2() { //Different booleans
        assertEquals(0D, calculateDistanceForNotEquals(false, true));
    }

    public void assertEquals(double aDouble, double otherDouble) {
        Assertions.assertEquals(aDouble, otherDouble, MIN_VALUE);
    }
}