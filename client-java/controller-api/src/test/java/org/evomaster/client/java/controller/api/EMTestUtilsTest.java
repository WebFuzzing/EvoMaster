package org.evomaster.client.java.controller.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class EMTestUtilsTest {

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
    public void givenAnInvalidLocationHeaderWhenResolveLocationThenTheExpectedTemplateIsReturned() {
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
