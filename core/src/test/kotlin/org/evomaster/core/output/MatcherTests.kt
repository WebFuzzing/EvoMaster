package org.evomaster.core.output

import org.evomaster.client.java.controller.contentMatchers.NumberMatcher.numbersMatch
import org.evomaster.client.java.controller.contentMatchers.StringMatcher.stringsMatch
import org.evomaster.client.java.controller.contentMatchers.SubStringMatcher.subStringsMatch
import org.evomaster.client.java.controller.contentMatchers.StringCollectionMatcher.collectionContains
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.util.*

class MatcherTests{

    /**
     * The [stringsMatch] should be true only if the two strings match exactly.
     */
    @Test
    fun testStringMatcher(){
        assertTrue(stringsMatch("test", "test"))
        assertTrue(stringsMatch("", ""))

        assertFalse(stringsMatch("testlonger", "test"))
        assertFalse(stringsMatch("test", "longertest"))
    }

    /**
     * The [subStringsMatch] should be true if one of the strings contains the other. This is used for those generated
     * tests that have been truncated for whatever reason (for example, removing timestamps or object references). In
     * those cases, exact matches would be hard to achieve.
     */
    @Test
    fun testSubStringMatcher(){
        assertTrue(subStringsMatch("test", "test"))
        assertTrue(subStringsMatch("test", "longertest"))
        assertTrue(subStringsMatch("longertest", "test"))

        assertFalse(subStringsMatch("test", "tst"))
        assertFalse(subStringsMatch("tst", "test"))
    }

    /**
     * The [numbersMatch] should be true if the representations are of the same number. Rest-assured, JSON, and all
     * have (at times) different representations of numbers. The point of this matcher is to conduct this comparison in
     * and easy manner.
     *
     * The matcher should be true if the numbers are equal, regardless of representation.
     */

    @Test
    fun testNumberMatcher(){
        assertTrue(numbersMatch(404, 404.0))
        assertTrue(numbersMatch(404.0, "404"))
        assertTrue(numbersMatch(404, "404.0"))

        assertFalse(numbersMatch(500, 404.0))
        assertFalse(numbersMatch(500, "404"))
        assertFalse(numbersMatch(404.0, "404.1"))
        assertFalse(numbersMatch(404.1, "404.0"))
    }

    /**
     * The [testStringCollectionMatcher]
     */
    @Test
    fun testStringCollectionMatcher(){
        assertTrue(collectionContains(Arrays.asList("mup", "tup", "vup"), "vup"))
        assertFalse(collectionContains(Arrays.asList("mup", "tup", "vup"), "mjup"))
    }


}