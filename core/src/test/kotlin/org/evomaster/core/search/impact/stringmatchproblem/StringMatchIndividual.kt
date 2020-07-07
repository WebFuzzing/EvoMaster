package org.evomaster.core.search.impact.stringmatchproblem

import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.StringGene
import org.evomaster.core.search.service.Randomness

/**
 * created by manzh on 2020-06-16
 */
class StringMatchIndividual (
        val gene : StringGene) :  Individual() {


    override fun seeActions(): List<out Action> = listOf()

    override fun verifyInitializationActions(): Boolean {
        //do nothing
        return true
    }

    override fun repairInitializationActions(randomness: Randomness) {
        //do nothing
    }

    override fun size(): Int = 1

    override fun seeGenes(filter: GeneFilter): List<out Gene> = listOf(gene)

    override fun copy(): Individual {
        return StringMatchIndividual(gene.copy() as StringGene)
    }
}