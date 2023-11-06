package org.evomaster.client.java.distance.heuristics;

import org.junit.jupiter.api.Test;

import static org.evomaster.client.java.distance.heuristics.DistanceHelper.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by arcuri82 on 26-Jun-19.
 */
public class DistanceHelperTest {


    @Test
    public void testConstants() {
        assertTrue(0 < H_REACHED_BUT_NULL);
        assertTrue(H_REACHED_BUT_NULL < H_NOT_NULL);
        assertTrue(H_NOT_NULL < 1);
    }

    @Test
    public void testDistanceDigit() {

        assertEquals(0, distanceToDigit('0'));
        assertEquals(0, distanceToDigit('1'));
        assertEquals(0, distanceToDigit('2'));
        assertEquals(0, distanceToDigit('3'));
        assertEquals(0, distanceToDigit('4'));
        assertEquals(0, distanceToDigit('5'));
        assertEquals(0, distanceToDigit('6'));
        assertEquals(0, distanceToDigit('7'));
        assertEquals(0, distanceToDigit('8'));
        assertEquals(0, distanceToDigit('9'));

        //see ascii table
        assertEquals(1, distanceToDigit('/'));
        assertEquals(2, distanceToDigit('.'));
        assertEquals(1, distanceToDigit(':'));
        assertEquals(2, distanceToDigit(';'));

        assertTrue(distanceToDigit('a') < distanceToDigit('b'));
    }

    @Test
    public void testIntegerDistance() {
        double distance = getDistanceToEquality(-10, 10);
        assertEquals(20, distance);
    }

    @Test
    public void testIntegerMaxDistance() {
        double distance = getDistanceToEquality(Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertEquals(Math.pow(2, 32) - 1, distance);
    }


    @Test
    public void testLongMaxDistance() {
        double distance = getDistanceToEquality(Long.MIN_VALUE, Long.MAX_VALUE);
        assertEquals(Math.pow(2, 64) - 1, distance);
    }

    @Test
    public void testDoubleOverflowsDistance() {
        double distance = getDistanceToEquality(-Double.MAX_VALUE, Double.MAX_VALUE);
        assertEquals(Double.MAX_VALUE, distance);
    }

    @Test
    public void testDoubleMaxDistance() {
        double upperBound = Double.MAX_VALUE  /2;
        double lowerBound = -upperBound;
        double distance = getDistanceToEquality(lowerBound, upperBound);
        assertEquals(Double.MAX_VALUE, distance);
    }
}