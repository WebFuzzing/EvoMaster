package org.evomaster.client.java.instrumentation.testabilityexception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.evomaster.client.java.instrumentation.testabilityexception.LocalDateExceptionHeuristics.*;

/**
 * Created by arcuri82 on 26-Jun-19.
 */
public class LocalDateExceptionHeuristicsTest {


    @Test
    public void testParseValid(){
        assertEquals(1d, parse("0001-01-01"));
        assertEquals(1d, parse("1982-11-27"));
        assertEquals(1d, parse("1970-01-01"));
        assertEquals(1d, parse("9999-12-31"));
    }

    @Test
    public void testParseTooShortLong(){

        double h0 = parse("1");
        double h1 = parse("1234-11-"); //2 shorter
        double ok = parse("1234-11-11"); //ok
        double h3 = parse("1234-11-111"); // 1 too long

        assertEquals(1d, ok);
        assertTrue(h0 < h1);
        assertTrue(h0 < ok);
        assertTrue(h0 < h3);
        assertTrue(h1 < ok);
        assertTrue(h3 < ok);
        assertTrue(h1 < h3);
    }

    @Test
    public void testParseNearlyCorrect(){

        /*
            recall ASCII:
            '-' -> 45
            '0' -> 48
            '9' -> 57
            'a' -> 97
         */

        double h0 = parse("a234-11-11");
        double h1 = parse("1234a11-11");
        double h2 = parse("1234-11a11");
        double h3 = parse("1234-11-aa");
        double h4 = parse("1234a11a11");

        assertTrue(h1 < h0);
        assertTrue(h2 < h0);
        assertEquals(h1 , h2);
        assertTrue(h3 < h1);
        assertTrue(h4 < h1);
        assertTrue(h4 < h3);
    }
}