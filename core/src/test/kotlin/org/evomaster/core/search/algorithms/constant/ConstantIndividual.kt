package org.evomaster.core.search.algorithms.constant

import org.evomaster.core.search.Individual
import org.evomaster.core.search.action.ActionFilter
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.service.Randomness

/**
 * Created by arcuri82 on 20-Feb-17.
 */
class ConstantIndividual(val action: ConstantAction) : Individual(children= mutableListOf(action)) {

    constructor(gene: IntegerGene) : this(ConstantAction(gene))

    override fun copyContent(): Individual {
        return ConstantIndividual(action.copy() as ConstantAction)
    }


    override fun seeTopGenes(filter: ActionFilter): List<out Gene> {
        return listOf(action.gene)
    }

    fun getGene() : IntegerGene = action.gene

    override fun size(): Int {
        return 1
    }


    override fun verifyInitializationActions(): Boolean {
        return true
    }

    override fun repairInitializationActions(randomness: Randomness) {

    }

}