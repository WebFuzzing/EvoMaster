package org.evomaster.core.search.gene.regex

import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CharacterClassEscapeRxGeneTest {

    @Test
    fun testRandomize() {
        val randomness = Randomness()
        val gene = CharacterClassEscapeRxGene("d")
        gene.randomize(randomness, forceNewValue = true, allGenes = listOf())
        assertTrue(gene.value.toInt() >= 0)
        assertTrue(gene.value.toInt() <= 9)
    }

    @Test
    fun testStandardMutation() {
        val randomness = Randomness()
        val gene = CharacterClassEscapeRxGene("d")
        val apc = AdaptiveParameterControl()
        gene.standardMutation(randomness, apc = apc, allGenes = listOf())
        assertTrue(gene.value.toInt() >= 0)
        assertTrue(gene.value.toInt() <= 9)
    }
}