package org.evomaster.client.java.sql.distance.advanced.helper;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static java.lang.Double.MAX_VALUE;
import static java.lang.Double.MIN_VALUE;
import static org.evomaster.client.java.sql.distance.advanced.helpers.distance.BranchDistanceHelper.*;

public class BranchDistanceHelperTest {

    @Test
    public void testEquals() { //Null objects
        assertEquals(MAX_VALUE, calculateDistanceForEquals(null, new Object()));
        assertEquals(MAX_VALUE, calculateDistanceForEquals(new Object(), null));
        assertEquals(0D, calculateDistanceForEquals(null, null));
    }

    @Test
    public void testEquals3() { //Numbers
        assertEquals(0D, calculateDistanceForEquals(1D, 1D));
        assertEquals(1D, calculateDistanceForEquals(1D, 2D));
    }

    @Test
    public void testEquals5() { //Booleans
        assertEquals(0D, calculateDistanceForEquals(true, true));
        assertEquals(1D, calculateDistanceForEquals(true, false));
    }

    @Test
    public void testEquals7() { //Boolean conversion
        assertEquals(0D, calculateDistanceForEquals(true, 1));
        assertEquals(0D, calculateDistanceForEquals(1, true));
    }

    @Test
    public void testNotEquals() { //Null objects
        assertEquals(0D, calculateDistanceForNotEquals(null, new Object()));
        assertEquals(0D, calculateDistanceForNotEquals(new Object(), null));
        assertEquals(MAX_VALUE, calculateDistanceForNotEquals(null, null));
    }

    @Test
    public void testNotEquals3() { //Numbers
        assertEquals(1D, calculateDistanceForNotEquals(1D, 1D));
        assertEquals(0D, calculateDistanceForNotEquals(1D, 2D));
    }

    @Test
    public void testNotEquals5() { //Booleans
        assertEquals(1D, calculateDistanceForNotEquals(true, true));
        assertEquals(0D, calculateDistanceForNotEquals(true, false));
    }

    @Test
    public void testNotEquals7() { //Boolean conversion
        assertEquals(1D, calculateDistanceForNotEquals(true, 1));
        assertEquals(1D, calculateDistanceForNotEquals(1, true));
    }

    @Test
    public void testGreaterThan() { //Null objects
        assertEquals(MAX_VALUE, calculateDistanceForGreaterThan(null, new Object()));
        assertEquals(MAX_VALUE, calculateDistanceForGreaterThan(new Object(), null));
    }

    @Test
    public void testGreaterThan2() { //Numbers
        assertEquals(0D, calculateDistanceForGreaterThan(2D, 1D));
        assertEquals(1D, calculateDistanceForGreaterThan(2D, 2D));
    }

    @Test
    public void testGreaterThanOrEquals() { //Null objects
        assertEquals(MAX_VALUE, calculateDistanceForGreaterThanOrEquals(null, new Object()));
        assertEquals(MAX_VALUE, calculateDistanceForGreaterThanOrEquals(new Object(), null));
    }

    @Test
    public void testGreaterThanOrEquals2() { //Numbers
        assertEquals(0D, calculateDistanceForGreaterThanOrEquals(2D, 1D));
        assertEquals(0D, calculateDistanceForGreaterThanOrEquals(2D, 2D));
        assertEquals(1D, calculateDistanceForGreaterThanOrEquals(2D, 3D));
    }

    @Test
    public void testMinor() { //Null objects
        assertEquals(MAX_VALUE, calculateDistanceForMinorThan(null, new Object()));
        assertEquals(MAX_VALUE, calculateDistanceForMinorThan(new Object(), null));
    }

    @Test
    public void testMinor2() { //Numbers
        assertEquals(0D, calculateDistanceForMinorThan(2D, 3D));
        assertEquals(1D, calculateDistanceForMinorThan(2D, 2D));
    }

    @Test
    public void testMinorThanOrEquals() { //Null objects
        assertEquals(MAX_VALUE, calculateDistanceForMinorThanOrEquals(null, new Object()));
        assertEquals(MAX_VALUE, calculateDistanceForMinorThanOrEquals(new Object(), null));
    }

    @Test
    public void testMinorThanOrEquals2() { //Numbers
        assertEquals(0D, calculateDistanceForMinorThanOrEquals(2D, 3D));
        assertEquals(0D, calculateDistanceForMinorThanOrEquals(2D, 2D));
        assertEquals(1D, calculateDistanceForMinorThanOrEquals(2D, 1D));
    }

    @Test
    public void testAnd() {
        assertEquals(3D, aggregateDistancesForAnd(1D, 2D));
    }

    @Test
    public void testOr() {
        assertEquals(1D, aggregateDistancesForOr(1D, 2D));
    }

    public void assertEquals(double aDouble, double otherDouble) {
        Assertions.assertEquals(aDouble, otherDouble, MIN_VALUE);
    }
}