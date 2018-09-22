package org.evomaster.experiments.pair

import org.evomaster.core.search.Action
import org.evomaster.core.search.Individual
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene


class PairIndividual(val x: IntegerGene, val y: IntegerGene) : Individual() {

    override fun copy(): Individual {
        return PairIndividual(x.copy() as IntegerGene, y.copy() as IntegerGene)
    }

    override fun seeGenes(filter: GeneFilter): List<out Gene> {
        return listOf(x,y)
    }

    override fun size(): Int {
        return 1
    }

    override fun seeActions(): List<out Action> {
        return listOf()
    }
}