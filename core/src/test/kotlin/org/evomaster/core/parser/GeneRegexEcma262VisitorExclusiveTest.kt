package org.evomaster.core.parser

import org.evomaster.core.search.gene.regex.RegexGene
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Tests for behavior that is specific to ECMA262 regex, and that is not valid for JVM regex.
 * Unlike [GeneRegexEcma262VisitorTest], this class is not extended by the tests for JVM regex.
 */
class GeneRegexEcma262VisitorExclusiveTest : RegexTestTemplate() {

    override fun createGene(regex: String): RegexGene {
        return RegexHandler.createGeneForEcma262(regex)
    }

    @Test
    fun testUnsatisfiableClassMakesItsAlternativeUnsatisfiable(){
        checkSamplesExactly("""^(a|[])$""", "a")
        checkSamplesExactly("""^(a|[^\s\S])$""", "a")
        checkSamplesExactly("""^a$|[^\u0000-\uffff]""", "a")
        checkSamplesExactly("""^([]|b|[^\d\D])$""", "b")
        checkSamplesExactly("""^((a|[])|b)$""", "a", "b")
        checkSamplesExactly("""^(a|[])b$""", "ab")
        checkSamplesExactly("""^(x([])|y)$""", "y")
        // not intersections
        checkSamplesExactly("""^(([a&&b]|[])|b)$""", "a", "b", "&")
        checkSamplesExactly("""^([a&&]|[])$""", "a", "&")
    }

    @Test
    fun testUnsatisfiableClassWithOptionalQuantifier(){
        // not having to appear at all makes it satisfiable
        checkSamplesExactly("""^a[]*b$""", "ab")
        checkSamplesExactly("""^[]?a$""", "a")
        checkSamplesExactly("""^x([])*y$""", "xy")
        checkSamplesExactly("""^a([]|)?b$""", "ab")
    }

    @Test
    fun testUnsatisfiableRegex(){
        // in JS these are valid, they just never match. So there is nothing to sample, as in Java regex
        listOf(
            """[]""",
            """^[]$""",
            """^[^\u0000-\uffff]$""",
            """[^\u0000-\uffff]""",
            """^[^\s\S]$""",
            """[^\d\D]""",
            """^[^\w\W]$""",
            """a[]b""",
            """[]+""",
            """^x([])y$""",
            """^([]|[^\s\S])$""",
            """[]{2}""",
            """([])+""",
            """[]|[]""",
            """^[]+$|[^\s\S]b"""
        ).forEach {
            assertThrows<IllegalStateException> { createGene(it) }
        }
    }

    @Test
    fun testBackReference(){
        checkSamplesExactly("""^(a)\1$""", "aa")
        checkSamplesExactly("""^(a|b)\1$""", "aa", "bb")
        checkSamplesExactly("""^(a)(b)\2\1$""", "abba")
        checkSamplesExactly("""^(?:(a)|b)c\1$""", "aca", "bc")
        // numbered as the opening parenthesis comes, and non-capturing groups do not count
        checkSamplesExactly("""^((a)|(b))\3$""", "a", "bb")
        checkSamplesExactly("""^(?:(a))(?:(b))\2$""", "abb")
        checkSamplesExactly("""^[(](a)\1$""", "(aa")
    }

    @Test
    fun testBackReferenceToGroupNotYetSeen(){
        // these references match ""
        checkSamplesExactly("""^\1(a)$""", "a")
        checkSamplesExactly("""^(a\1)$""", "a")
        checkSamplesExactly("""^(a)\2(b)$""", "ab")
    }

    @Test
    fun testBackReferenceToGroupThatDidNotTakePart(){
        // these references match ""
        checkSamplesExactly("""^([^\s\S])*\1$""", "")
        checkSamplesExactly("""^(a)?\1$""", "", "aa")
        checkSamplesExactly("""^(?:(a)|b)\1$""", "aa", "b")
    }

    @Test
    fun testBackReferenceUsesAllDigits(){
        checkSamplesExactly("""^(a)(a)(a)(a)(a)(a)(a)(a)(a)(a)\10$""", "aaaaaaaaaaa")
        // only 1 group, so \10 is the legacy octal escape (U+0008), and not \1 followed by 0
        checkSamplesExactly("""^(a)\10$""", "a\u0008")
    }

    @Test
    fun testLegacyOctalWhenNotEnoughGroups(){
        checkSamplesExactly("""^\1$""", "\u0001")
        checkSamplesExactly("""^\0$""", "\u0000")
        checkSamplesExactly("""^\101$""", "A")
        checkSamplesExactly("""^\12\377$""", "\n\u00ff")
        checkSamplesExactly("""^(?:a)\1$""", "a\u0001")
        checkSamplesExactly("""^(a)\2$""", "a\u0002")
        checkSamplesExactly("""^(a)\12$""", "a\n")
        checkSamplesExactly("""^(a)\19$""", "a\u00019")
        checkSamplesExactly("""^(a)\400$""", "a 0")
        // an escaped parenthesis is not a group
        checkSamplesExactly("""^\(a\1$""", "(a\u0001")
    }

    @Test
    fun testEightAndNineAreLiteralWhenNotGroups(){
        checkSamplesExactly("""^\8$""", "8")
        checkSamplesExactly("""^\9\8$""", "98")
        checkSamplesExactly("""^\81$""", "81")
        checkSamplesExactly("""^(a)\8$""", "a8")
        checkSamplesExactly("""^\08$""", "\u00008")
    }

    @Test
    fun testDigitsAfterOctalEscapeAreSeparateCharacters(){
        checkSamplesExactly("""^\18$""", "\u00018")
        checkSamplesExactly("""^(a)\18$""", "a\u00018")
        checkSamplesExactly("""^(a)\108$""", "a\u00088") // \108 is not octal so \10 followed by 8
        // the quantifier is for the 8 only
        checkCanSample("""^\18*$""", listOf("\u0001", "\u00018", "\u000188"), 1000)
        checkCanSample("""^\18+$""", listOf("\u00018", "\u000188"), 1000)
    }

    @Test
    fun testDecimalEscapesInClass(){
        // never a backreference in a class, even if the group referenced exists
        checkSamplesExactly("""^(a)[\1]$""", "a\u0001")
        checkCanSample("""^(a)(b)(c)[\1-\377]$""", listOf("abc\u0001", "abca", "abc\u00ff"), 5000)
        checkSamplesExactly("""^(a)()[\1\2]$""", "a\u0001", "a\u0002")
        checkSamplesExactly("""^(a)()()()()()()()[\8]$""", "a8") // \8 is identity escape, "8"
        checkCanSample("""^[\000-\377]$""", listOf("\u0000", "\u0001", "a", "\u007f", "\u00ff"), 5000)
        checkSamplesExactly("""^[\0-\7]$""", "\u0000", "\u0001", "\u0002", "\u0003", "\u0004", "\u0005", "\u0006", "\u0007")
        // the leftover digit 8 is part of the range 8-9
        checkSamplesExactly("""^[\18-9]$""", "\u0001", "8", "9")
    }

    @Test
    fun testBackReferenceInQuantifier(){
        checkCanSample("""^(a|b)\1+$""", listOf("aa", "aaa", "bb", "bbb"), 1000)
        checkSamplesExactly("""^(1)(2)(3)(4)(5)(6)(7)(8L)\8{2}$""", "12345678L8L8L")
    }

    @Test
    fun testBackReferenceToUnsatisfiableGroup(){
        // the alternative with the group that cannot match is dropped, so the group does not take part and \1 is empty
        checkSamplesExactly("""^(?:([^\s\S])|a)\1$""", "a")
        checkSamplesExactly("""^(?:([^\s\S])\1|a)$""", "a")
        checkSamplesExactly("""^(?:([^\s\S])+|a)\1$""", "a")
        // a quantifier that allows 0 repetitions of the group, and so the group does not take part
        checkSamplesExactly("""^([^\s\S])*b\1$""", "b")
        checkSamplesExactly("""^([^\s\S])?b\1$""", "b")
        checkSamplesExactly("""^([^\s\S]){0,2}b\1$""", "b")
        // here \2 is the group (a), even if group 1 is unsatisfiable
        checkSamplesExactly("""^(?:([^\s\S])|(a))\2$""", "aa")
        checkSamplesExactly("""^(?:([^\s\S])|(a))\1$""", "a")
        // when the unsatisfiable group has to take part, there is nothing that matches
        assertThrows<IllegalStateException> { createGene("""^([^\s\S])\1$""") }
        assertThrows<IllegalStateException> { createGene("""^([^\s\S])+\1$""") }
        assertThrows<IllegalStateException> { createGene("""^(?:([^\s\S])|[^\s\S])\1$""") }
    }

    @Test
    fun testCaptureGroupNumbers(){
        checkSamplesExactly("""^(?:(a)(?:b)((c)(?:d))|(e))\1\2\3\4$""", "abcdacdc", "ee")
    }
}
