package org.evomaster.core.search.gene.regex

import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.evomaster.core.parser.RegexType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.regex.Pattern

class CharacterClassEscapeRxGeneTest {

    @Test
    fun testRandomize() {
        val randomness = Randomness()
        for (i in 1..1000) {
            val gene = CharacterClassEscapeRxGene("d")
            gene.randomize(randomness, tryToForceNewValue = true)
            assertTrue(gene.value.toInt() >= 0)
            assertTrue(gene.value.toInt() <= 9)
        }
    }

    @Test
    fun testStandardMutation() {
        val randomness = Randomness()
        for (i in 1..1000) {
            val gene = CharacterClassEscapeRxGene("d")
            val apc = AdaptiveParameterControl()
            val mwc = MutationWeightControl()
            gene.doInitialize(randomness)
            gene.standardMutation(randomness, apc = apc, mwc = mwc, childrenToMutateSelectionStrategy = SubsetGeneMutationSelectionStrategy.DEFAULT)
            assertTrue(gene.value.toInt() >= 0, "invalid digit value: " + gene.value)
            assertTrue(gene.value.toInt() <= 90, "invalid digit value: " + gene.value)
        }
    }

    @Test
    fun `Calling standardMutation() before randomize() should throw IllegalStateException`() {
        val randomness = Randomness()
        val gene = CharacterClassEscapeRxGene("d")
        val apc = AdaptiveParameterControl()
        val mwc = MutationWeightControl()
        assertThrows<IllegalStateException>("standardMutation() cannot be successful without calling to randomize() first",
                { gene.standardMutation(randomness, apc = apc, mwc = mwc, childrenToMutateSelectionStrategy = SubsetGeneMutationSelectionStrategy.DEFAULT) })

    }

    @Test
    fun test_w(){

        assertTrue(Pattern.matches("\\w", "a"))
        assertTrue(Pattern.matches("\\w", "1"))
        assertTrue(Pattern.matches("\\w", "_"))
        assertFalse(Pattern.matches("\\w", "ø"))
        assertFalse(Pattern.matches("\\w", "!"))
        assertFalse(Pattern.matches("\\w", " "))
        assertFalse(Pattern.matches("\\w", "\n"))

        val randomness = Randomness()
        for (i in 1..1000) {
            val gene = CharacterClassEscapeRxGene("w")
            gene.randomize(randomness, true)
            val k = gene.getValueAsPrintableString()
            assertTrue(Pattern.matches("\\w", k))
        }
    }

    @Test
    fun test_W(){

        assertFalse(Pattern.matches("\\W", "a"))
        assertFalse(Pattern.matches("\\W", "1"))
        assertFalse(Pattern.matches("\\W", "_"))
        assertTrue(Pattern.matches("\\W", "ø"))
        assertTrue(Pattern.matches("\\W", "!"))
        assertTrue(Pattern.matches("\\W", " "))
        assertTrue(Pattern.matches("\\W", "\n"))

        val randomness = Randomness()
        for (i in 1..1000) {
            val gene = CharacterClassEscapeRxGene("W")
            gene.randomize(randomness, true)
            val k = gene.getValueAsPrintableString()
            assertTrue(Pattern.matches("\\W", k))
        }
    }

    @Test
    fun testEcma262SpaceHasExpectedCharacters() {
        val ecma262Spaces = listOf(
            ' ', '\t', '\n', '\u000b', '\u000c', '\r', '\u00a0', '\u1680', '\u2028', '\u2029', '\u202f', '\u205f',
            '\u3000', '\ufeff'
        ) + ('\u2000'..'\u200a')

        val space = CharacterClassEscapeRxGene("s", regexType = RegexType.ECMA_262).multiCharRange
        val nonSpace = CharacterClassEscapeRxGene("S", regexType = RegexType.ECMA_262).multiCharRange

        ecma262Spaces.forEach {
            assertTrue(space.contains(it), "\\s should contain ${it.code}")
            assertFalse(nonSpace.contains(it), "\\S should not contain ${it.code}")
        }
        // not white space in ECMA
        listOf('\u0085', '\u180e', '\u200b', '\u2060').forEach {
            assertFalse(space.contains(it), "\\s should not contain ${it.code}")
            assertTrue(nonSpace.contains(it), "\\S should contain ${it.code}")
        }
    }

    @Test
    fun testJavaSpaceIsNotChanged() {
        val space = CharacterClassEscapeRxGene("s").multiCharRange
        val nonSpace = CharacterClassEscapeRxGene("S").multiCharRange
        assertEquals(6, space.charCount)
        assertFalse(space.contains('\u00a0'))
        assertFalse(space.contains('\u2028'))
        assertTrue(nonSpace.contains('\u00a0'))
    }

    @Test
    fun testEcma262SpaceValidityDoesNotUseJava() {
        val space = CharacterClassEscapeRxGene("s", regexType = RegexType.ECMA_262)
        val nonSpace = CharacterClassEscapeRxGene("S", regexType = RegexType.ECMA_262)

        // java.util.regex would say the opposite for all of these
        listOf('\u00a0', '\u2003', '\ufeff').forEach {
            space.setValueWithRawString(it.toString())
            nonSpace.setValueWithRawString(it.toString())
            assertTrue(space.isLocallyValid(), "\\s should accept ${it.code}")
            assertFalse(nonSpace.isLocallyValid(), "\\S should not accept ${it.code}")
        }
        listOf('a', '\u0085', '\u180e').forEach {
            space.setValueWithRawString(it.toString())
            nonSpace.setValueWithRawString(it.toString())
            assertFalse(space.isLocallyValid(), "\\s should not accept ${it.code}")
            assertTrue(nonSpace.isLocallyValid(), "\\S should accept ${it.code}")
        }
        space.setValueWithRawString("  ")
        assertFalse(space.isLocallyValid())
    }

    @Test
    fun testEcma262SpaceIsKeptWhenCopied() {
        val randomness = Randomness().apply { updateSeed(42) }
        val gene = CharacterClassEscapeRxGene("s", regexType = RegexType.ECMA_262)
        repeat(100) {
            gene.randomize(randomness, false)
            val copy = gene.copy() as CharacterClassEscapeRxGene
            assertEquals(RegexType.ECMA_262, copy.regexType)
            assertEquals(gene.getValueAsRawString(), copy.getValueAsRawString())
            assertTrue(copy.isLocallyValid())
        }
    }
}