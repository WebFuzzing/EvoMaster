package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.junit.jupiter.api.Test;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes.LocalDateClassReplacement.parseHeuristic;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by arcuri82 on 26-Jun-19.
 */
public class LocalDateClassReplacementTest {


    @Test
    public void testParseValid(){
        assertEquals(1d, parseHeuristic("0001-01-01"));
        assertEquals(1d, parseHeuristic("1982-11-27"));
        assertEquals(1d, parseHeuristic("1970-01-01"));
        assertEquals(1d, parseHeuristic("9999-12-31"));
    }

    @Test
    public void testParseTooShortLong(){

        double h0 = parseHeuristic("1");
        double h1 = parseHeuristic("1234-11-"); //2 shorter
        double ok = parseHeuristic("1234-11-11"); //ok
        double h3 = parseHeuristic("1234-11-111"); // 1 too long

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

        double h0 = parseHeuristic("a234-11-11");
        double h1 = parseHeuristic("1234a11-11");
        double h2 = parseHeuristic("1234-11a11");
        double h3 = parseHeuristic("1234-11-aa");
        double h4 = parseHeuristic("1234a11a11");

        assertTrue(h1 < h0);
        assertTrue(h2 < h0);
        assertEquals(h1 , h2);
        assertTrue(h3 < h1);
        assertTrue(h4 < h1);
        assertTrue(h4 < h3);
    }
}