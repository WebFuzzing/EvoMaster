package org.evomaster.core.search.matchproblem


import org.evomaster.core.search.Action
import org.evomaster.core.search.gene.Gene

class PrimitiveTypeMatchAction(gene: Gene, localId : String = NONE_ACTION_COMPONENT_ID) : Action(listOf(gene), localId) {
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
        return PrimitiveTypeMatchAction(children.filterIsInstance<Gene>().first().copy(), getLocalId())
    }
}