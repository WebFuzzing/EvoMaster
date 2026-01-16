package org.evomaster.client.java.sql.heuristic.function;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CoalesceFunctionTest {

    @Test
    void testReturnsFirstNonNullArgument() {
        final CoalesceFunction coalesceFunction = new CoalesceFunction();
        Object result = coalesceFunction.evaluate("first", "second", "third");
        assertEquals("first", result);
    }

    @Test
    void testReturnsSecondArgumentWhenFirstIsNull() {
        final CoalesceFunction coalesceFunction = new CoalesceFunction();
        Object result = coalesceFunction.evaluate(null, "second", "third");
        assertEquals("second", result);
    }

    @Test
    void testReturnsLastArgumentWhenAllPreviousAreNull() {
        final CoalesceFunction coalesceFunction = new CoalesceFunction();
        Object result = coalesceFunction.evaluate(null, null, null, "last");
        assertEquals("last", result);
    }

    @Test
    void testReturnsNullWhenAllArgumentsAreNull() {
        final CoalesceFunction coalesceFunction = new CoalesceFunction();
        Object result = coalesceFunction.evaluate(null, null, null);
        assertNull(result);
    }

    @Test
    void testWorksWithSingleNonNullArgument() {
        final CoalesceFunction coalesceFunction = new CoalesceFunction();
        Object result = coalesceFunction.evaluate("only");
        assertEquals("only", result);
    }

    @Test
    void testReturnsNullWithSingleNullArgument() {
        final CoalesceFunction coalesceFunction = new CoalesceFunction();
        Object result = coalesceFunction.evaluate((Object) null);
        assertNull(result);
    }

    @Test
    void testThrowsExceptionWhenNoArguments() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new CoalesceFunction().evaluate()
        );
        assertEquals("COALESCE() function requires at least one argument", exception.getMessage());
    }

    @Test
    void testWorksWithIntegerArguments() {
        final CoalesceFunction coalesceFunction = new CoalesceFunction();
        Object result = coalesceFunction.evaluate(null, 42, 100);
        assertEquals(42, result);
    }

    @Test
    void testWorksWithMixedTypes() {
        final CoalesceFunction coalesceFunction = new CoalesceFunction();
        Object result = coalesceFunction.evaluate(null, null, 123, "string");
        assertEquals(123, result);
    }

    @Test
    void testWorksWithBooleanArguments() {
        final CoalesceFunction coalesceFunction = new CoalesceFunction();
        Object result = coalesceFunction.evaluate(null, true, false);
        assertEquals(true, result);
    }

    @Test
    void testReturnsFirstNonNullEvenIfZero() {
        final CoalesceFunction coalesceFunction = new CoalesceFunction();
        Object result = coalesceFunction.evaluate(null, 0, 42);
        assertEquals(0, result);
    }

    @Test
    void testReturnsFirstNonNullEvenIfEmptyString() {
        final CoalesceFunction coalesceFunction = new CoalesceFunction();
        Object result = coalesceFunction.evaluate(null, "", "non-empty");
        assertEquals("", result);
    }

    @Test
    void testReturnsFirstNonNullEvenIfFalse() {
        final CoalesceFunction coalesceFunction = new CoalesceFunction();
        Object result = coalesceFunction.evaluate(null, false, true);
        assertEquals(false, result);
    }

}
