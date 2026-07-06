package org.evomaster.test.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EMTestUtilsTest {

    @Test
    public void testCreateString(){

        String prefix = "foo";
        String postfix = "bar";
        int min = 5;
        int max = 10;

        String first = EMTestUtils.createString(min, max, prefix, postfix);
        assertTrue(first.startsWith(prefix));
        assertTrue(first.endsWith(postfix));
        assertTrue(first.length() >= min);
        assertTrue(first.length() <= max);

        String second = EMTestUtils.createString(min, max, prefix, postfix);
        assertTrue(second.startsWith(prefix));
        assertTrue(second.endsWith(postfix));
        assertTrue(second.length() >= min);
        assertTrue(second.length() <= max);

        assertNotEquals(first, second);
    }


    @Test
    public void testEmptyPath(){

        String template = "http://localhost:12345/a/42";
        String location = "/a/";

        String res = EMTestUtils.resolveLocation(location, template);
        assertEquals("http://localhost:12345/a/", res);
    }


    @Test
    public void testResolveLocation_direct() {

        String template = "http://localhost:12345/a/{id}";
        String location = "/a/5";

        String res = EMTestUtils.resolveLocation(location, template);
        assertEquals("http://localhost:12345/a/5", res);
    }

    @Test
    public void testResolveLocation_indirect() {

        String template = "http://localhost:12345/a/{id}/x";
        String location = "/a/5";

        String res = EMTestUtils.resolveLocation(location, template);
        assertEquals("http://localhost:12345/a/5/x", res);
    }

    @Test
    public void testResolveLocation_fullURI_different_indirect() {

        String template = "http://localhost:12345/a/{id}/x";
        String location = "https://127.0.0.1:80/a/5";

        String res = EMTestUtils.resolveLocation(location, template);
        assertEquals("https://127.0.0.1:80/a/5/x", res);
    }

    @Test
    public void testResolveLocation_null() {
        String template = "http://localhost:12345/a/x";
        String location = null;

        String res = EMTestUtils.resolveLocation(location, template);
        assertEquals(template, res);
    }

    @Test
    public void testGivenAnInvalidLocationHeaderWhenResolveLocationThenTheExpectedTemplateIsReturned() {
        String expectedTemplate = "http://localhost:12345/a/x";
        String locationHeader = "/a/\"52\"";

        String resolvedLocation = EMTestUtils.resolveLocation(locationHeader, expectedTemplate);

        assertEquals(locationHeader, resolvedLocation);
    }

    @Test
    public void testIsValidURI() {

        assertTrue(EMTestUtils.isValidURIorEmpty(null));
        assertTrue(EMTestUtils.isValidURIorEmpty("    "));
        assertTrue(EMTestUtils.isValidURIorEmpty("a"));
        assertTrue(EMTestUtils.isValidURIorEmpty("/a"));
        assertTrue(EMTestUtils.isValidURIorEmpty("/a/b"));
        assertTrue(EMTestUtils.isValidURIorEmpty("/a/b/c?k=4&z=foo"));
        assertTrue(EMTestUtils.isValidURIorEmpty("http://foo.org/a"));
        assertTrue(EMTestUtils.isValidURIorEmpty("https://127.0.0.1:443"));

        //this should fail, as "{}" are invalid chars
        assertFalse(EMTestUtils.isValidURIorEmpty("/{a}"));
    }

}
