package org.evomaster.core.search.algorithms.constant

import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene
import org.evomaster.core.search.service.Randomness

/**
 * Created by arcuri82 on 20-Feb-17.
 */
class ConstantIndividual(val gene: IntegerGene) : Individual() {

    override fun copy(): Individual {
        return ConstantIndividual(gene.copy() as IntegerGene)
    }

    override fun seeGenes(filter: GeneFilter): List<out Gene> {
        return listOf(gene)
    }

    override fun size(): Int {
        return 1
    }

    override fun seeActions(): List<out Action> {
        return listOf()
    }

    override fun verifyInitializationActions(): Boolean {
        return true
    }

    override fun repairInitializationActions(randomness: Randomness) {

    }

}