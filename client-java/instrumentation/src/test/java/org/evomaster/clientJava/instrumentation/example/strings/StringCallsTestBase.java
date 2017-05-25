package org.evomaster.clientJava.instrumentation.example.strings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public abstract class StringCallsTestBase {

    protected abstract StringCalls getInstance() throws Exception;

    @Test
    public void test_equals_firstNull() throws Exception {
        StringCalls sc = getInstance();
        assertThrows(NullPointerException.class, () -> sc.callEquals(null, "foo"));
    }

    @Test
    public void test_equals_secondNull() throws Exception {
        StringCalls sc = getInstance();
        assertFalse(sc.callEquals("foo", null));
    }

    @Test
    public void test_equals_true() throws Exception {
        StringCalls sc = getInstance();
        assertTrue(sc.callEquals("foo", "foo"));
    }

    @Test
    public void test_equals_false() throws Exception {
        StringCalls sc = getInstance();
        assertFalse(sc.callEquals("foo", "bar"));
    }

    @Test
    public void test_equals_noToString() throws Exception {
        StringCalls sc = getInstance();
        String a = "foo";
        StringBuffer b = new StringBuffer(a);

        assertTrue(sc.callEquals(a, b.toString()));
        assertFalse(sc.callEquals(a, b));
    }

    @Test
    public void test_equalsIgnoreCase_firstNull() throws Exception {
        StringCalls sc = getInstance();
        assertThrows(NullPointerException.class, () -> sc.callEqualsIgnoreCase(null, "foo"));
    }

    @Test
    public void test_equalsIgnoreCase_secondNull() throws Exception {
        StringCalls sc = getInstance();
        assertFalse(sc.callEqualsIgnoreCase("foo", null));
    }

    @Test
    public void test_equalsIgnoreCase_true() throws Exception {
        StringCalls sc = getInstance();
        assertTrue(sc.callEqualsIgnoreCase("FoO", "fOo"));
    }

    @Test
    public void test_equalsIgnoreCase_false() throws Exception {
        StringCalls sc = getInstance();
        assertFalse(sc.callEqualsIgnoreCase("foo", "bar"));
    }


    @Test
    public void test_StartsWith_firstNull() throws Exception {
        StringCalls sc = getInstance();
        assertThrows(NullPointerException.class, () -> sc.callStartsWith(null, "foo"));
    }

    @Test
    public void test_StartsWith_secondNull() throws Exception {
        StringCalls sc = getInstance();
        assertThrows(NullPointerException.class, () -> sc.callStartsWith("foo", null));
    }

    @Test
    public void test_StartsWith_true() throws Exception {
        StringCalls sc = getInstance();
        assertTrue(sc.callStartsWith("foo", "f"));
    }

    @Test
    public void test_StartsWith_false() throws Exception {
        StringCalls sc = getInstance();
        assertFalse(sc.callStartsWith("foo", "bar"));
        assertFalse(sc.callStartsWith("f", "bar"));
    }

    @Test
    public void test_StartsWith_offset() throws Exception {
        StringCalls sc = getInstance();
        assertTrue(sc.callStartsWith("foo", "o", 1));
        assertTrue(sc.callStartsWith("foo", "o", 2));
        assertFalse(sc.callStartsWith("foo", "o", -1));
        assertFalse(sc.callStartsWith("foo", "o", 0));
        assertFalse(sc.callStartsWith("foo", "o", 3));
    }

    @Test
    public void test_EndsWith_firstNull() throws Exception {
        StringCalls sc = getInstance();
        assertThrows(NullPointerException.class, () -> sc.callEndsWith(null, "foo"));
    }

    @Test
    public void test_EndsWith_secondNull() throws Exception {
        StringCalls sc = getInstance();
        assertThrows(NullPointerException.class, () -> sc.callEndsWith("foo", null));
    }

    @Test
    public void test_EndsWith_true() throws Exception {
        StringCalls sc = getInstance();
        assertTrue(sc.callEndsWith("foo", "o"));
    }

    @Test
    public void test_EndsWith_false() throws Exception {
        StringCalls sc = getInstance();
        assertFalse(sc.callEndsWith("foo", "f"));
    }


    @Test
    public void test_IsEmpty_null() throws Exception {
        StringCalls sc = getInstance();
        assertThrows(NullPointerException.class, () -> sc.callIsEmpty(null));
    }

    @Test
    public void test_IsEmpty_true() throws Exception {
        StringCalls sc = getInstance();
        assertTrue(sc.callIsEmpty(""));
    }

    @Test
    public void test_IsEmpty_false() throws Exception {
        StringCalls sc = getInstance();
        assertFalse(sc.callIsEmpty(" "));
    }

    @Test
    public void test_ContentEquals_firstNull() throws Exception {
        StringCalls sc = getInstance();
        assertThrows(NullPointerException.class, () -> sc.callContentEquals(null, "foo"));
        assertThrows(NullPointerException.class, () -> sc.callContentEquals(null, new StringBuffer("foo")));
    }

    @Test
    public void test_ContentEquals_secondNull() throws Exception {
        StringCalls sc = getInstance();
        assertThrows(NullPointerException.class, () -> sc.callContentEquals("foo", (CharSequence) null));
        assertThrows(NullPointerException.class, () -> sc.callContentEquals("foo", (StringBuffer) null));
    }

    @Test
    public void test_ContentEquals_true() throws Exception {
        StringCalls sc = getInstance();
        assertTrue(sc.callContentEquals("foo", "foo"));
        assertTrue(sc.callContentEquals("foo", new StringBuffer("foo")));
    }

    @Test
    public void test_ContentEquals_false() throws Exception {
        StringCalls sc = getInstance();
        assertFalse(sc.callContentEquals("foo", "bar"));
        assertFalse(sc.callContentEquals("foo", new StringBuffer("bar")));
    }


    @Test
    public void test_Contains_firstNull() throws Exception {
        StringCalls sc = getInstance();
        assertThrows(NullPointerException.class, () -> sc.callContains(null, "foo"));
    }

    @Test
    public void test_Contains_secondNull() throws Exception {
        StringCalls sc = getInstance();
        assertThrows(NullPointerException.class, () -> sc.callContains("foo", null));
    }

    @Test
    public void test_Contains_true() throws Exception {
        StringCalls sc = getInstance();
        assertTrue(sc.callContains("foo", ""));
        assertTrue(sc.callContains("foo", "f"));
        assertTrue(sc.callContains("foo", "fo"));
        assertTrue(sc.callContains("foo", "foo"));
        assertTrue(sc.callContains("foo", "oo"));
        assertTrue(sc.callContains("foo", "o"));
    }

    @Test
    public void test_Contains_false() throws Exception {
        StringCalls sc = getInstance();
        assertFalse(sc.callContains("foo", "bar"));
        assertFalse(sc.callContains("foo", "foooo"));
    }
}
