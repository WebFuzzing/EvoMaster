package org.evomaster.test.utils;

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
        assertTrue(EMTestUtils.isValidURIorEmpty("http://.foo.org/a"));
        assertTrue(EMTestUtils.isValidURIorEmpty("https://127.0.0.1:443"));

        assertTrue(EMTestUtils.isValidURIorEmpty("http://example.com"));
        assertTrue(EMTestUtils.isValidURIorEmpty("mailto:user@domain.com"));
        assertTrue(EMTestUtils.isValidURIorEmpty("ssh://user@git.openstack.org:29418/openstack/keystone.git"));
        assertTrue(EMTestUtils.isValidURIorEmpty("--://///{a}"));
        assertTrue(EMTestUtils.isValidURIorEmpty("http://.foo.org/a"));
        assertTrue(EMTestUtils.isValidURIorEmpty("http://foo.org/#"));
        assertTrue(EMTestUtils.isValidURIorEmpty("http://foo.org/#a"));

        //this should fail, as "{}" are invalid chars
        assertTrue(EMTestUtils.isValidURIorEmpty("/{a}"));

        assertTrue(EMTestUtils.isValidURIorEmpty("http://.example.com"));
        assertTrue(EMTestUtils.isValidURIorEmpty("http://example.com/|foo"));
        assertTrue(EMTestUtils.isValidURIorEmpty("http://example.com/?key=value&"));
        assertTrue(EMTestUtils.isValidURIorEmpty("http://example.com/#fragment?query=value"));
        assertTrue(EMTestUtils.isValidURIorEmpty("http://example.com:port"));
//
    }

}
