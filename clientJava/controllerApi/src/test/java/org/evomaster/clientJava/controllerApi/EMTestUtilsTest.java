package org.evomaster.clientJava.controllerApi;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class EMTestUtilsTest {

    @Test
    public void testResolveLocation() {
        //TODO
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