package org.evomaster.client.java.sql.heuristic.function;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LowerFunctionTest {

    @Test
    void testEvaluateWithValidString() {
        LowerFunction lowerFunction = new LowerFunction();
        assertEquals("test", lowerFunction.evaluate("TEST"));
        assertEquals("test", lowerFunction.evaluate("test"));
        assertEquals("123", lowerFunction.evaluate("123"));
        assertEquals("", lowerFunction.evaluate(""));
    }

    @Test
    void testEvaluateWithNull() {
        LowerFunction lowerFunction = new LowerFunction();
        assertNull(lowerFunction.evaluate((Object) null));
    }

    @Test
    void testEvaluateWithNoArguments() {
        LowerFunction lowerFunction = new LowerFunction();
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> lowerFunction.evaluate());
        assertTrue(exception.getMessage().contains("exactly one argument"));
    }

    @Test
    void testEvaluateWithTooManyArguments() {
        LowerFunction lowerFunction = new LowerFunction();
        assertThrows(IllegalArgumentException.class, () -> lowerFunction.evaluate("a", "b"));
    }

    @Test
    void testEvaluateWithNonStringArgument() {
        LowerFunction lowerFunction = new LowerFunction();
        assertThrows(IllegalArgumentException.class, () -> lowerFunction.evaluate(123));
    }
}
