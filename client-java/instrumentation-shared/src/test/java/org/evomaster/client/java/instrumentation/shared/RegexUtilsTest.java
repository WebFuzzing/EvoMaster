package org.evomaster.client.java.instrumentation.shared;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegexUtilsTest {


    @Disabled
    @Test
    public static void testParentheses(){

        String s = "foo";
        assertEquals("foo", RegexSharedUtils.removeParentheses(s));
        assertEquals("foo", RegexSharedUtils.removeParentheses("("+s+")"));
        assertEquals("foo", RegexSharedUtils.removeParentheses("(("+s+"))"));

        String x = "bar";
        assertEquals("(foo)(bar)", RegexSharedUtils.removeParentheses("("+s+")("+x+")"));
        assertEquals("(foo)(bar)", RegexSharedUtils.removeParentheses("(("+s+")("+x+"))"));
    }

}