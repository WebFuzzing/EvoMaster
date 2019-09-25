package org.evomaster.core.search.gene.regex

import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CharacterClassEscapeRxGeneTest {

    @Test
    fun testRandomize() {
        val randomness = Randomness()
        for (i in 1..1000) {
            val gene = CharacterClassEscapeRxGene("d")
            gene.randomize(randomness, forceNewValue = true, allGenes = listOf())
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
            gene.randomize(randomness, forceNewValue = true, allGenes = listOf())
            gene.standardMutation(randomness, apc = apc, allGenes = listOf())
            assertTrue(gene.value.toInt() >= 0, "invalid digit value: " + gene.value)
            assertTrue(gene.value.toInt() <= 90, "invalid digit value: " + gene.value)
        }
    }

    @Test
    fun `Calling standardMutation() before randomize() should throw IllegalStateException`() {
        val randomness = Randomness()
        val gene = CharacterClassEscapeRxGene("d")
        val apc = AdaptiveParameterControl()
        assertThrows<IllegalStateException>("standardMutation() cannot be successfull without calling to randomize() first",
                { gene.standardMutation(randomness, apc = apc, allGenes = listOf()) })

    }
}