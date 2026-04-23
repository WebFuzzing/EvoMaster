package org.evomaster.solver.smtlib.assertion;

import org.evomaster.solver.smtlib.*;
import org.evomaster.solver.smtlib.assertion.*;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.com.google.common.collect.ImmutableList;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class AssertionTest {

    @Test
    public void testGetAssertion() {
        // Create a mock Assertion object
        Assertion assertion = new Assertion();

        // Create an AssertSMTNode with the mock Assertion
        AssertSMTNode assertSMTNode = new AssertSMTNode(assertion);

        // Verify that getAssertion returns the correct Assertion
        assertEquals(assertion, assertSMTNode.getAssertion());
    }

    @Test
    public void testAssertionToString() {
        Assertion assertion = new EqualsAssertion(ImmutableList.of("a", "b"));
        assertEquals("(= a b)", assertion.toString());
    }

    @Test
    public void testAndAssertionToString() {
        Assertion a = new EqualsAssertion(ImmutableList.of("a", "b"));
        Assertion b = new GreaterThanAssertion("x", "y");
        AndAssertion andAssertion = new AndAssertion(ImmutableList.of(a, b));
        assertEquals("(and (= a b) (> x y))", andAssertion.toString());
    }

    @Test
    public void testAndAssertionFailure() {
        Assertion a = new EqualsAssertion(ImmutableList.of("a", "b"));
        try {
            new AndAssertion(Collections.singletonList(a));
            fail("AndAssertion should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("And must have at least two assertions", e.getMessage());
        }
    }

    @Test
    public void testDistinctAssertionToString() {
        DistinctAssertion assertion = new DistinctAssertion(ImmutableList.of("a", "b"));
        AssertSMTNode node = new AssertSMTNode(assertion);
        String expected = "(assert (distinct a b))";
        assertEquals(expected, node.toString());
    }

    @Test
    public void testDistinctAssertionFailure() {
        try {
            new DistinctAssertion(Collections.singletonList("a"));
            fail("DistinctAssertion should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Distinct must have at least two variables", e.getMessage());
        }
    }

    @Test
    public void testEqualsAssertionToString() {
        EqualsAssertion equalsAssertion = new EqualsAssertion(ImmutableList.of("a", "b"));
        assertEquals("(= a b)", equalsAssertion.toString());
    }

    @Test
    public void testEqualsAssertionFailure() {
        try {
            new EqualsAssertion(Collections.singletonList("a"));
            fail("EqualsAssertion should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Equals must have at least two variables", e.getMessage());
        }
    }

    @Test
    public void testGreaterThanAssertionToString() {
        GreaterThanAssertion gtAssertion = new GreaterThanAssertion("x", "y");
        assertEquals("(> x y)", gtAssertion.toString());
    }

    @Test
    public void testGreaterThanOrEqualsAssertionToString() {
        GreaterThanOrEqualsAssertion gteAssertion = new GreaterThanOrEqualsAssertion("x", "y");
        assertEquals("(>= x y)", gteAssertion.toString());
    }
    @Test
    public void testLessThanAssertionToString() {
        LessThanAssertion ltAssertion = new LessThanAssertion("x", "y");
        assertEquals("(< x y)", ltAssertion.toString());
    }

    @Test
    public void testLessThanOrEqualsAssertionToString() {
        LessThanOrEqualsAssertion lteAssertion = new LessThanOrEqualsAssertion("x", "y");
        assertEquals("(<= x y)", lteAssertion.toString());
    }

    @Test
    public void testOrAssertionToString() {
        Assertion a = new EqualsAssertion(ImmutableList.of("a", "b"));
        Assertion b = new GreaterThanAssertion("x", "y");
        OrAssertion orAssertion = new OrAssertion(ImmutableList.of(a, b));
        assertEquals("(or (= a b) (> x y))", orAssertion.toString());
    }

    @Test
    public void testOrAssertionFailure() {
        try {
            new OrAssertion(Collections.singletonList(new EqualsAssertion(ImmutableList.of("a", "b"))));
            fail("OrAssertion should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("And must have at least two assertions", e.getMessage());
        }
    }
}