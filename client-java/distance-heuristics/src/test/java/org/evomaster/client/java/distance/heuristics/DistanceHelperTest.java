package org.evomaster.client.java.distance.heuristics;

import org.junit.jupiter.api.Test;

import java.util.UUID;

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
        double upperBound = Double.MAX_VALUE / 2;
        double lowerBound = -upperBound;
        double distance = getDistanceToEquality(lowerBound, upperBound);
        assertEquals(Double.MAX_VALUE, distance);
    }

    @Test
    public void testDistanceUUIDEquals() {
        UUID left = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        UUID right = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        double distance = getDistance(left, right);
        assertEquals(0, distance);
    }

    @Test
    public void testDistanceUUIDNotEquals() {
        UUID left = UUID.fromString("123e4567-e89b-12d3-a456-426614174001");
        UUID right = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        double distance = getDistance(left, right);
        assertEquals(1, distance);
    }

    @Test
    public void testDistanceUUIDNotEqualsThree() {
        UUID left = UUID.fromString("123e4567-e89b-12d3-a456-426614174003");
        UUID right = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        double distance = getDistance(left, right);
        assertEquals(2, distance);
    }

    @Test
    public void testByteArrayLeftAlignmentDistance() {
        byte[] argentinaHomeShirt = new byte[]{10, 20, 30};
        byte[] sameShirt = new byte[]{10, 20, 30};
        byte[] oneChangedColor = new byte[]{10, 20, 31};
        byte[] allChangedColors = new byte[]{11, 21, 31};
        byte[] missingColor = new byte[]{10, 20};

        assertEquals(0, getLeftAlignmentDistance(argentinaHomeShirt, sameShirt));
        assertEquals(1, getLeftAlignmentDistance(argentinaHomeShirt, oneChangedColor));
        assertEquals(3, getLeftAlignmentDistance(argentinaHomeShirt, allChangedColors));
        assertEquals(1, getLeftAlignmentDistance(argentinaHomeShirt, missingColor));
        assertEquals(1, getLeftAlignmentDistance(missingColor, argentinaHomeShirt));
        assertEquals(1, getLeftAlignmentDistance(new byte[]{0}, new byte[]{-1}));
        assertEquals(4, getLeftAlignmentDistance(
                new byte[]{1, 2, 3}, new byte[]{0, 1, 2, 3}));
        assertEquals(0, getLeftAlignmentDistance(new byte[0], new byte[0]));
        assertThrows(NullPointerException.class,
                () -> getLeftAlignmentDistance(null, argentinaHomeShirt));
        assertThrows(NullPointerException.class,
                () -> getLeftAlignmentDistance(argentinaHomeShirt, null));
    }
}
