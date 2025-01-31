package org.evomaster.client.java.sql.heuristic;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlStringUtilsTest {

    @Test
    void testRemovesSingleQuotes() {
        String input = "'Hello'";
        String expected = "Hello";
        String actual = SqlStringUtils.removeEnclosingQuotes(input);
        assertEquals(expected, actual);
    }

    @Test
    void testRemovesDoubleQuotes() {
        String input = "\"Hello\"";
        String expected = "Hello";
        String actual = SqlStringUtils.removeEnclosingQuotes(input);
        assertEquals(expected, actual);
    }

    @Test
    void testRemovesSingleQuotesWithSpaces() {
        String input = "' Hello '";
        String expected = " Hello ";
        String actual = SqlStringUtils.removeEnclosingQuotes(input);
        assertEquals(expected, actual);
    }

    @Test
    void testRemovesDoubleQuotesWithSpaces() {
        String input = "\" Hello \"";
        String expected = " Hello ";
        String actual = SqlStringUtils.removeEnclosingQuotes(input);
        assertEquals(expected, actual);
    }

    @Test
    void testNoQuotesToRemove() {
        String input = "Hello";
        String expected = "Hello";
        String actual = SqlStringUtils.removeEnclosingQuotes(input);
        assertEquals(expected, actual);
    }

    @Test
    void testEmptyString() {
        String input = "";
        String expected = "";
        String actual = SqlStringUtils.removeEnclosingQuotes(input);
        assertEquals(expected, actual);
    }

    @Test
    void testNullInput() {
        String input = null;
        String actual = SqlStringUtils.removeEnclosingQuotes(input);
        assertNull(actual);
    }

    @Test
    void testSingleCharacter() {
        String input = "'";
        String expected = "'";
        String actual = SqlStringUtils.removeEnclosingQuotes(input);
        assertEquals(expected, actual);
    }

    @Test
    void testOnlyStartingQuote() {
        String input = "'Hello";
        String expected = "'Hello";
        String actual = SqlStringUtils.removeEnclosingQuotes(input);
        assertEquals(expected, actual);
    }

    @Test
    void testOnlyEndingQuote() {
        String input = "Hello'";
        String expected = "Hello'";
        String actual = SqlStringUtils.removeEnclosingQuotes(input);
        assertEquals(expected, actual);
    }

    @Test
    void testMixedQuotesNoRemoval() {
        String input = "'Hello\"";
        String expected = "'Hello\"";
        String actual = SqlStringUtils.removeEnclosingQuotes(input);
        assertEquals(expected, actual);
    }

    @Test
    void testSingleCharacterDoubleQuote() {
        String input = "\"";
        String expected = "\"";
        String actual = SqlStringUtils.removeEnclosingQuotes(input);
        assertEquals(expected, actual);
    }

}

