package org.evomaster.clientJava.controllerApi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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