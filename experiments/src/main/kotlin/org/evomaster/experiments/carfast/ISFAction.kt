package org.evomaster.experiments.carfast

import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.IntegerGene


class ISFAction(val methodName: String, val genes: List<IntegerGene>) : Action{

    override fun getName(): String {
        return methodName
    }

    override fun seeGenes(): List<out Gene> {
        return genes
    }

    override fun copy(): Action {
        return ISFAction(methodName, genes.map { g -> g.copy() as IntegerGene })
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return true
    }
}