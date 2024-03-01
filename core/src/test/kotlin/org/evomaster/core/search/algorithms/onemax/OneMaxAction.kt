package org.evomaster.core.search.algorithms.onemax

import org.evomaster.core.search.action.Action
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.Gene

class OneMaxAction(val list : MutableList<EnumGene<Double>> = mutableListOf()) : Action(list) {
    override fun getName(): String {
        return "OneMax Action"
    }

    override fun seeTopGenes(): List<out Gene> {
        return list
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return true
    }

    override fun copyContent(): StructuralElement {
       return OneMaxAction(list.map{ it.copy() as EnumGene<Double> }.toMutableList())
    }
}