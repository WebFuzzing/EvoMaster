package org.evomaster.clientJava.instrumentation.example.strings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class StringsExampleTestBase {

    protected abstract StringsExample getInstance() throws Exception;

    @Test
    public void test_isFooWithDirectReturn() throws Exception{

        StringsExample se = getInstance();
        assertTrue(se.isFooWithDirectReturn("foo"));
        assertFalse(se.isFooWithDirectReturn("bar"));
    }

    @Test
    public void test_isFooWithDirectReturnUsingReplacement() throws Exception{

        StringsExample se = getInstance();
        assertTrue(se.isFooWithDirectReturnUsingReplacement("foo"));
        assertFalse(se.isFooWithDirectReturnUsingReplacement("bar"));
    }

    @Test
    public void test_isFooWithBooleanCheck() throws Exception{

        StringsExample se = getInstance();
        assertTrue(se.isFooWithBooleanCheck("foo"));
        assertFalse(se.isFooWithBooleanCheck("bar"));
    }

    @Test
    public void test_isFooWithNegatedBooleanCheck() throws Exception{

        StringsExample se = getInstance();
        assertTrue(se.isFooWithNegatedBooleanCheck("foo"));
        assertFalse(se.isFooWithNegatedBooleanCheck("bar"));
    }



    @Test
    public void test_isFooWithIf() throws Exception{

        StringsExample se = getInstance();
        assertTrue(se.isFooWithIf("foo"));
        assertFalse(se.isFooWithIf("bar"));
    }

    @Test
    public void test_isFooWithLocalVariable() throws Exception{

        StringsExample se = getInstance();
        assertTrue(se.isFooWithLocalVariable("foo"));
        assertFalse(se.isFooWithLocalVariable("bar"));
    }

    @Test
    public void test_isFooWithLocalVariableInIf() throws Exception{

        StringsExample se = getInstance();
        assertTrue(se.isFooWithLocalVariableInIf("foo"));
        assertFalse(se.isFooWithLocalVariableInIf("bar"));
    }

    @Test
    public void test_isNotFooWithLocalVariable() throws Exception{

        StringsExample se = getInstance();
        assertFalse(se.isNotFooWithLocalVariable("foo"));
        assertTrue(se.isNotFooWithLocalVariable("bar"));
    }

    @Test
    public void test_isBarWithPositiveX() throws Exception{

        StringsExample se = getInstance();
        assertTrue(se.isBarWithPositiveX("bar", 5));
        assertFalse(se.isBarWithPositiveX("bar", -5));
        assertFalse(se.isBarWithPositiveX("foo", 5));
    }
}
