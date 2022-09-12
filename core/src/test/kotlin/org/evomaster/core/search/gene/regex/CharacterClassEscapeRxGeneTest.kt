package org.evomaster.core.search.gene.regex

import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
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


}