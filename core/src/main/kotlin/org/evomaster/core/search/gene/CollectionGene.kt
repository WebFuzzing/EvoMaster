package org.evomaster.core.search.gene

import org.evomaster.core.search.impact.impactInfoCollection.CollectionImpact
import org.evomaster.core.search.impact.impactInfoCollection.Impact
import org.evomaster.core.search.service.mutator.geneMutation.SubsetGeneSelectionStrategy

/**
 * created by manzh on 2020-06-06
 */
interface CollectionGene {
    fun defaultProbabilityToModifySize() : Double =  0.1

    fun probabilityToModifySize(selectionStrategy: SubsetGeneSelectionStrategy, impact: Impact?) : Double {
        if (selectionStrategy != SubsetGeneSelectionStrategy.ADAPTIVE_WEIGHT) return defaultProbabilityToModifySize()
        impact?:return  defaultProbabilityToModifySize()
        if (impact !is CollectionImpact) return  defaultProbabilityToModifySize()

        return if (impact.recentImprovementOnSize()) defaultProbabilityToModifySize() * timesProbToModifySize() else defaultProbabilityToModifySize()
    }

    fun timesProbToModifySize() : Int = 3
}