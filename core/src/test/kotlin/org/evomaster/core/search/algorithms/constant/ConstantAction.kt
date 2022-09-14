package org.evomaster.core.search.algorithms.constant

import org.evomaster.core.search.Action
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.numeric.IntegerGene

class ConstantAction(val gene: IntegerGene, localId : String = NONE_ACTION_COMPONENT_ID
) : Action(mutableListOf(gene), localId){

    override fun getName(): String {
        return "ConstantAction Action"
    }

    override fun seeTopGenes(): List<out Gene> {
        return listOf(gene)
    }

    override fun shouldCountForFitnessEvaluations(): Boolean {
        return true
    }

    override fun copyContent(): StructuralElement {
        return ConstantAction(gene.copy() as IntegerGene, getLocalId())
    }
}