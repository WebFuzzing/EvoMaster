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
}
