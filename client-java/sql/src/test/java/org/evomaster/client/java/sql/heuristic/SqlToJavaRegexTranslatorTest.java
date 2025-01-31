package org.evomaster.client.java.sql.heuristic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SqlToJavaRegexTranslatorTest {

    @Test
    void translatesSimpleLikePattern() {
        String input = "Hello%";
        String expected = "Hello.*";
        String actual = new SqlToJavaRegexTranslator().translateLikePattern(input);
        assertEquals(expected, actual);
    }

    @Test
    void translatesLikePatternWithUnderscore() {
        String input = "H_llo";
        String expected = "H.llo";
        String actual = new SqlToJavaRegexTranslator().translateLikePattern(input);
        assertEquals(expected, actual);
    }

    @Test
    void translatesLikePatternWithEscapedPercent() {
        String input = "Hello\\%";
        String expected = "Hello%";
        String actual = new SqlToJavaRegexTranslator().translateLikePattern(input);
        assertEquals(expected, actual);
    }

    @Test
    void translatesLikePatternWithEscapedUnderscore() {
        String input = "H\\_llo";
        String expected = "H_llo";
        String actual = new SqlToJavaRegexTranslator().translateLikePattern(input);
        assertEquals(expected, actual);
    }

    @Test
    void translatesLikePatternWithMixedWildcards() {
        String input = "H\\_ll%o";
        String expected = "H_ll.*o";
        String actual = new SqlToJavaRegexTranslator().translateLikePattern(input);
        assertEquals(expected, actual);
    }

    @Test
    void translatesEmptyLikePattern() {
        String input = "";
        String expected = "";
        String actual = new SqlToJavaRegexTranslator().translateLikePattern(input);
        assertEquals(expected, actual);
    }

    @Test
    void translatesNullLikePattern() {
        String input = null;
        assertThrows(NullPointerException.class , ()->new SqlToJavaRegexTranslator().translateLikePattern(input));
    }

    @Test
    void translatesLikePatternWithOnlyEscapeSymbol() {
        String input = "\\";
        String expected = "\\";
        String actual = new SqlToJavaRegexTranslator().translateLikePattern(input);
        assertEquals(expected, actual);
    }

    @Test
    void translatesLikePatternWithMultipleEscapedWildcards() {
        String input = "H\\_ll\\%o";
        String expected = "H_ll%o";
        String actual = new SqlToJavaRegexTranslator().translateLikePattern(input);
        assertEquals(expected, actual);
    }
}
