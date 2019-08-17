package org.evomaster.client.java.instrumentation.deprecated_testabilityexception;

import org.junit.jupiter.api.Test;

import static org.evomaster.client.java.instrumentation.deprecated_testabilityexception.ExceptionHeuristics.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by arcuri82 on 26-Jun-19.
 */
public class ExceptionHeuristicsTest {


    @Test
    public void testConstants(){
        assertTrue(0 < H_REACHED_BUT_NULL);
        assertTrue(H_REACHED_BUT_NULL < H_NOT_NULL);
        assertTrue(H_NOT_NULL < 1);
    }

    @Test
    public void testDistanceDigit(){

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
}