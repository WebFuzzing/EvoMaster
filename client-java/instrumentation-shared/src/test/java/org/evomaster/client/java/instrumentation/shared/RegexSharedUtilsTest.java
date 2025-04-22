package org.evomaster.client.java.instrumentation.shared;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegexSharedUtilsTest {

    @Test
    void translatesSimpleLikePattern() {
        String input = "Hello%";
        String expected = "Hello.*";
        String actual = RegexSharedUtils.translateSqlLikePattern(input);
        assertEquals(expected, actual);
    }

    @Test
    void translatesLikePatternWithUnderscore() {
        String input = "H_llo";
        String expected = "H.llo";
        String actual = RegexSharedUtils.translateSqlLikePattern(input);
        assertEquals(expected, actual);
    }

    @Test
    void translatesLikePatternWithEscapedPercent() {
        String input = "Hello\\%";
        String expected = "Hello%";
        String actual = RegexSharedUtils.translateSqlLikePattern(input);
        assertEquals(expected, actual);
    }

    @Test
    void translatesLikePatternWithEscapedUnderscore() {
        String input = "H\\_llo";
        String expected = "H_llo";
        String actual = RegexSharedUtils.translateSqlLikePattern(input);
        assertEquals(expected, actual);
    }

    @Test
    void translatesLikePatternWithMixedWildcards() {
        String input = "H\\_ll%o";
        String expected = "H_ll.*o";
        String actual = RegexSharedUtils.translateSqlLikePattern(input);
        assertEquals(expected, actual);
    }

    @Test
    void translatesEmptyLikePattern() {
        String input = "";
        String expected = "";
        String actual = RegexSharedUtils.translateSqlLikePattern(input);
        assertEquals(expected, actual);
    }

    @Test
    void translatesNullLikePattern() {
        String input = null;
        assertThrows(NullPointerException.class, () -> RegexSharedUtils.translateSqlLikePattern(input));
    }

    @Test
    void translatesLikePatternWithOnlyEscapeSymbol() {
        String input = "\\";
        String expected = "\\";
        String actual = RegexSharedUtils.translateSqlLikePattern(input);
        assertEquals(expected, actual);
    }

    @Test
    void translatesLikePatternWithMultipleEscapedWildcards() {
        String input = "H\\_ll\\%o";
        String expected = "H_ll%o";
        String actual = RegexSharedUtils.translateSqlLikePattern(input);
        assertEquals(expected, actual);
    }

    @Test
    void testPostgresToJavaRegex() {
        String input = "hello world";
        String expected = "hello world";
        String actual = RegexSharedUtils.translateSqlSimilarToPattern(input);
        assertEquals(expected, actual);
    }

    @Test
    void testWildcard1() {
        String input = "%hello%";
        String expected = ".*hello.*";
        String actual = RegexSharedUtils.translateSqlSimilarToPattern(input);
        assertEquals(expected, actual);
    }

    @Test
    void testWildcard2() {
        String input = "_hello_";
        String expected = ".hello.";
        String actual = RegexSharedUtils.translateSqlSimilarToPattern(input);
        assertEquals(expected, actual);
    }

    @Test
    void testEscape1() {
        String input = "\\%hello\\%";
        String expected = "%hello%";
        String actual = RegexSharedUtils.translateSqlSimilarToPattern(input);
        assertEquals(expected, actual);
    }

    @Test
    void testEscape2() {
        String input = "\\_hello\\_";
        String expected = "_hello_";
        String actual = RegexSharedUtils.translateSqlSimilarToPattern(input);
        assertEquals(expected, actual);
    }

    @Test
    void testEscape3() {
        String input = "\\\\";
        String expected = "\\";
        String actual = RegexSharedUtils.translateSqlSimilarToPattern(input);
        assertEquals(expected, actual);
    }

}
