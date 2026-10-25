package org.evomaster.core.search.gene.utils

import org.evomaster.core.parser.RegexHandler
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.regex.DisjunctionListRxGene
import org.evomaster.core.search.gene.regex.PatternCharacterBlockGene
import org.evomaster.core.search.gene.regex.QuantifierRxGene
import org.evomaster.core.search.service.Randomness
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RxGeneUtilsTest {

    private fun createGene(regex: String) = RegexHandler.createGeneForJVM(regex)

    private fun disjunctionsOf(regex: String): DisjunctionListRxGene =
        createGene(regex).disjunctions

    private fun groupNumbers(genes: List<Gene>): Set<Int> =
        genes.mapNotNull { (it as? DisjunctionListRxGene)?.captureGroupIndex }.toSet()


    @Test
    fun testFlatViewWithQuantifierTemplatesSameAsFlatViewWhenNoQuantifiersAreUsed(){
        val tree = disjunctionsOf("""^()(?=(a))(a)(b)(?:c(d)|(?:e)f)ghi|jk(lm)[nro-q](?:r)(s)$""")
        assertEquals(tree.flatView(), RxGeneUtils.flatViewWithQuantifierTemplates(tree))
    }

    @Test
    fun testTemplatesOfQuantifiersAreIncluded() {
        val tree = disjunctionsOf("""^(?:(a)|b)*(c)?(?:(?:(d))*e)+$""")

        // quantifiers have no children as they were not initialized, because of that no groups appear
        assertTrue(tree.flatView().filterIsInstance<QuantifierRxGene>().all { it.getViewOfChildren().isEmpty() })
        assertEquals(emptySet<Int>(), groupNumbers(tree.flatView()))

        // using flatViewWithQuantifierTemplates allows us to see the template genes and their groups without initializing
        assertEquals(setOf(1, 2, 3), groupNumbers(RxGeneUtils.flatViewWithQuantifierTemplates(tree)))
        assertEquals(setOf(1, 2, 3), RxGeneUtils.captureGroupIndexesIn(tree))

        // normal flatView does not see the pattern characters unless the quantifier adds atoms as children
        val flatViewResult = tree.flatView().filterIsInstance<PatternCharacterBlockGene>()
        val flatViewWithQuantifierTemplatesResult =
            RxGeneUtils.flatViewWithQuantifierTemplates(tree).filterIsInstance<PatternCharacterBlockGene>()
        assertTrue(flatViewResult.isEmpty())
        assertTrue(flatViewWithQuantifierTemplatesResult.isNotEmpty())

        val patternChars = flatViewWithQuantifierTemplatesResult.map { it.stringBlock[0] }
        assertTrue(patternChars.containsAll(listOf('a', 'b', 'c', 'd', 'e')))
    }

    @Test
    fun testSameGroupsEvenWhenTheQuantifiersHaveAtoms() {
        val gene = createGene("""^(?:(a)|b)*(c)?(?:(?:(d))*e)+$""")
        val randomness = Randomness()
        randomness.updateSeed(42)
        (1..20).forEach { _ ->
            assertEquals(setOf(1, 2, 3), RxGeneUtils.captureGroupIndexesIn(gene.disjunctions))
            gene.randomize(randomness, false)
        }
    }

    @Test
    fun testGroupsThatAreNotInQuantifiers() {
        assertEquals(setOf(1, 2, 3), RxGeneUtils.captureGroupIndexesIn(disjunctionsOf("""^(a(b))(c)$""")))
    }

    @Test
    fun testNonCapturingGroupsAreNotCounted() {
        assertEquals(emptySet<Int>(), RxGeneUtils.captureGroupIndexesIn(disjunctionsOf("""^(?:a|(?:b)+)*c$""")))
        assertEquals(setOf(1), RxGeneUtils.captureGroupIndexesIn(disjunctionsOf("""^(?:(a))+(?:b)$""")))
    }

    @Test
    fun testGroupIsInTheQuantifierItself() {
        val quantifier = disjunctionsOf("""^(?:(?:(a))*b)+$""").flatView().filterIsInstance<QuantifierRxGene>().first()
        assertEquals(setOf(1), RxGeneUtils.captureGroupIndexesIn(quantifier))
    }
}
