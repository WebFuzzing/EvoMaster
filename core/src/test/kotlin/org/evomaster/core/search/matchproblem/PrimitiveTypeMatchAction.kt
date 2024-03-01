package org.evomaster.core.search.matchproblem


import org.evomaster.core.search.action.Action
import org.evomaster.core.search.gene.Gene

class PrimitiveTypeMatchAction(gene: Gene) : Action(listOf(gene)) {
    override fun getName(): String {
        return "PrimitiveTypeMatchAction"
    }

    override fun seeTopGenes(): List<out Gene> {
        return children.filterIsInstance<Gene>()
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return true
    }

    override fun copyContent(): PrimitiveTypeMatchAction {
        return PrimitiveTypeMatchAction(children.filterIsInstance<Gene>().first().copy())
    }
}