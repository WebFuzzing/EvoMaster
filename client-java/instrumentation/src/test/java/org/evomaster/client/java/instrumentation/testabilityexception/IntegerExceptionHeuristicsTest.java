package org.evomaster.client.java.instrumentation.testabilityexception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.evomaster.client.java.instrumentation.testabilityexception.IntegerExceptionHeuristics.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by arcuri82 on 26-Jun-19.
 */
public class IntegerExceptionHeuristicsTest {

    @Test
    public void testParseValid(){
        assertEquals(1d, parseInt("1"));
        assertEquals(1d, parseInt("10"));
        assertEquals(1d, parseInt("123"));
        assertEquals(1d, parseInt("-1"));
        assertEquals(1d, parseInt("001"));
        assertEquals(1d, parseInt("-002"));
    }

    @Test
    public void testParseNull(){
        assertEquals(0d, parseInt(null));
    }

    @Test
    public void testParseEmpty(){

        double hnull = parseInt(null);
        double hempty = parseInt("");
        double hone = parseInt("1");

        assertTrue(hempty > hnull);
        assertTrue(hempty < hone);
    }

    @Test
    public void testParseInvalid(){

        double ha  = parseInt("a");

        assertTrue(ha > 0);
        assertTrue(ha < 1);
    }

    @Test
    public void testParseLongerInvalid(){

        double h0 = parseInt("a");
        double h1 = parseInt("a1");
        double h2 = parseInt("a1a");
        double h3 = parseInt("a1a1111");

        assertEquals(h0, h1);
        assertTrue(h1 > h2);
        assertTrue(h1 > h3);
    }

    @Test
    public void testParseTooLong(){

        double h0 = parseInt("a");
        double h1 = parseInt("a111111111111111111");

        assertTrue(h1 < 1);
        assertTrue(h0 > h1);
    }
}