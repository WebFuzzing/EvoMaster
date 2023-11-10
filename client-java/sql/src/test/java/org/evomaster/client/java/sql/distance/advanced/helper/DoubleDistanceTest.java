package org.evomaster.client.java.sql.distance.advanced.helper;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static java.lang.Double.MIN_VALUE;
import static org.evomaster.client.java.sql.distance.advanced.helpers.distance.DoubleDistanceHelper.*;

public class DoubleDistanceTest {

    @Test
    public void testEquals() { //Equal numbers
        assertEquals(0D, calculateDistanceForEquals(1D, 1D));
    }

    @Test
    public void testEquals2() { //Different numbers
        assertEquals(1D, calculateDistanceForEquals(1D, 0D));
    }

    @Test
    public void testNotEquals() { //Equal numbers
        assertEquals(1D, calculateDistanceForNotEquals(1D, 1D));
    }

    @Test
    public void testNotEquals2() { //Different numbers
        assertEquals(0D, calculateDistanceForNotEquals(1D, 0D));
    }

    @Test
    public void testGreaterThan() { //Number is greater
        assertEquals(0D, calculateDistanceForGreaterThan(2D, 1D));
    }

    @Test
    public void testGreaterThan2() { //Number is minor or equal
        assertEquals(1D, calculateDistanceForGreaterThan(2D, 2D));
        assertEquals(2D, calculateDistanceForGreaterThan(2D, 3D));
    }

    @Test
    public void testGreaterThanOrEquals() { //Number is greater or equal
        assertEquals(0D, calculateDistanceForGreaterThanOrEquals(2D, 1D));
        assertEquals(0D, calculateDistanceForGreaterThanOrEquals(2D, 2D));
    }

    @Test
    public void testGreaterThanOrEquals2() { //Number is minor
        assertEquals(1D, calculateDistanceForGreaterThanOrEquals(2D, 3D));
    }

    @Test
    public void testMinorThan() { //Number is minor
        assertEquals(0D, calculateDistanceForMinorThan(2D, 3D));
    }

    @Test
    public void testMinorThan2() { //Number is minor or equal
        assertEquals(1D, calculateDistanceForMinorThan(2D, 2D));
        assertEquals(2D, calculateDistanceForMinorThan(2D, 1D));
    }

    @Test
    public void testMinorThanOrEquals() { //Number is minor or equal
        assertEquals(0D, calculateDistanceForMinorThanOrEquals(2D, 3D));
        assertEquals(0D, calculateDistanceForMinorThanOrEquals(2D, 2D));
    }

    @Test
    public void testMinorThanOrEquals2() { //Number is minor
        assertEquals(1D, calculateDistanceForMinorThanOrEquals(2D, 1D));
    }

    public void assertEquals(double aDouble, double otherDouble) {
        Assertions.assertEquals(aDouble, otherDouble, MIN_VALUE);
    }
}