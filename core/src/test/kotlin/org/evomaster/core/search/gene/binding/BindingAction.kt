package org.evomaster.core.search.gene.binding


import org.evomaster.core.search.action.Action
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene

class BindingAction(genes: List<out Gene>) : Action(genes) {

    override fun getName(): String {
        return "Binding action"
    }

    override fun seeTopGenes(): List<out Gene> {
        return children as List<out Gene>
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return true
    }

    override fun copyContent(): StructuralElement {
        return BindingAction(children as List<out Gene>)
    }
}