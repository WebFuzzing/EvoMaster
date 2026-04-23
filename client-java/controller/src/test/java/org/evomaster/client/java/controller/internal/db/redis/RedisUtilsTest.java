package org.evomaster.client.java.controller.internal.db.redis;

import org.evomaster.client.java.controller.redis.RedisUtils;
import org.junit.jupiter.api.Test;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class RedisUtilsTest {

    @Test
    void testNullAndEmptyPatterns() {
        assertEquals(".*", RedisUtils.redisPatternToRegex(null));
        assertEquals(".*", RedisUtils.redisPatternToRegex(""));
    }

    @Test
    void testWildcardStar() {
        String regex = RedisUtils.redisPatternToRegex("foo*");
        assertTrue("foo".matches(regex));
        assertTrue("foobar".matches(regex));
        assertTrue("foobarbaz".matches(regex));
        assertFalse("fo".matches(regex));
    }

    @Test
    void testWildcardQuestionMark() {
        String regex = RedisUtils.redisPatternToRegex("h?llo");
        assertTrue("hello".matches(regex));
        assertTrue("hallo".matches(regex));
        assertFalse("hllo".matches(regex));
        assertFalse("heallo".matches(regex));
    }

    @Test
    void testBracketSimpleSet() {
        String regex = RedisUtils.redisPatternToRegex("h[ae]llo");
        assertTrue("hello".matches(regex));
        assertTrue("hallo".matches(regex));
        assertFalse("hollo".matches(regex));
    }

    @Test
    void testBracketNegatedSet() {
        String regex = RedisUtils.redisPatternToRegex("h[^e]llo");
        assertTrue("hallo".matches(regex));
        assertTrue("hollo".matches(regex));
        assertFalse("hello".matches(regex));
    }

    @Test
    void testBracketRange() {
        String regex = RedisUtils.redisPatternToRegex("file[0-9]");
        assertTrue("file1".matches(regex));
        assertTrue("file9".matches(regex));
        assertFalse("filea".matches(regex));
    }

    @Test
    void testEscapingRegexMetacharacters() {
        String regex = RedisUtils.redisPatternToRegex("price+(usd)");
        assertTrue("price+(usd)".matches(regex));
        assertFalse("price-usd".matches(regex));
    }

    @Test
    void testComplexPatternWithStarAndBrackets() {
        String regex = RedisUtils.redisPatternToRegex("user:[0-9]*");
        assertTrue("user:123".matches(regex));
        assertTrue("user:0".matches(regex));
        assertTrue("user:9999".matches(regex));
        assertFalse("user:a".matches(regex));
        assertFalse("usr:123".matches(regex));
    }

    @Test
    void testNestedEscapesAndBrackets() {
        String regex = RedisUtils.redisPatternToRegex("a\\[test\\]");
        assertEquals("^a\\[test\\]$", regex);
        System.out.println(regex);
    }

    @Test
    void testEscapedBrackets() {
        String regex = RedisUtils.redisPatternToRegex("a\\[test");
        assertEquals("^a\\[test$", regex);
        regex = RedisUtils.redisPatternToRegex("test\\]");
        assertEquals("^test\\]$", regex);
    }

    @Test
    void testAnchorsAdded() {
        String regex = RedisUtils.redisPatternToRegex("foo*");
        assertTrue(regex.startsWith("^"));
        assertTrue(regex.endsWith("$"));
    }

    @Test
    void testMultipleWildcards() {
        String regex = RedisUtils.redisPatternToRegex("*mid*dle*");
        assertTrue("middle".matches(regex));
        assertTrue("amidxxdlezzz".matches(regex));
        assertFalse("middl".matches(regex));
    }

    @Test
    void testSpecialCharactersEscapedProperly() {
        String regex = RedisUtils.redisPatternToRegex("a.b+c{d}|e^f$g");
        assertTrue(Pattern.matches(regex, "a.b+c{d}|e^f$g"));
        assertFalse(Pattern.matches(regex, "ab+c{d}|e^f$g"));
    }
}