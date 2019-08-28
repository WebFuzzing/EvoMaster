package org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes;

import org.junit.jupiter.api.Test;

import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.DistanceHelper.H_REACHED_BUT_NULL;
import static org.evomaster.client.java.instrumentation.coverage.methodreplacement.classes.IntegerClassReplacement.parseIntHeuristic;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by arcuri82 on 26-Jun-19.
 */
public class IntegerClassReplacementTest {

    @Test
    public void testParseValid(){
        assertEquals(1d, parseIntHeuristic("1"));
        assertEquals(1d, parseIntHeuristic("10"));
        assertEquals(1d, parseIntHeuristic("123"));
        assertEquals(1d, parseIntHeuristic("-1"));
        assertEquals(1d, parseIntHeuristic("001"));
        assertEquals(1d, parseIntHeuristic("-002"));
    }

    @Test
    public void testParseNull(){
        assertEquals(H_REACHED_BUT_NULL, parseIntHeuristic(null));
    }

    @Test
    public void testParseEmpty(){

        double hnull = parseIntHeuristic(null);
        double hempty = parseIntHeuristic("");
        double hone = parseIntHeuristic("1");

        assertTrue(hempty > hnull);
        assertTrue(hempty < hone);
    }

    @Test
    public void testParseInvalid(){

        double ha  = parseIntHeuristic("a");

        assertTrue(ha > 0);
        assertTrue(ha < 1);
    }

    @Test
    public void testParseLongerInvalid(){

        double h0 = parseIntHeuristic("a");
        double h1 = parseIntHeuristic("a1");
        double h2 = parseIntHeuristic("a1a");
        double h3 = parseIntHeuristic("a1a1111");

        assertEquals(h0, h1);
        assertTrue(h1 > h2);
        assertTrue(h1 > h3);
    }

    @Test
    public void testParseTooLong(){

        double h0 = parseIntHeuristic("a");
        double h1 = parseIntHeuristic("a111111111111111111");

        assertTrue(h1 < 1);
        assertTrue(h0 > h1);
    }
}