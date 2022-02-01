package org.evomaster.core.search.gene

import org.evomaster.core.search.impact.impactinfocollection.CollectionImpact
import org.evomaster.core.search.impact.impactinfocollection.Impact
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy

/**
 * created by manzh on 2020-06-06
 */
interface CollectionGene {

    /**
     * default probability of modifying size of the gene
     */
    fun defaultProbabilityToModifySize() : Double =  0.1

    /**
     * a impact-based probability fo modifying size of the gene
     */
    fun probabilityToModifySize(selectionStrategy: SubsetGeneSelectionStrategy, impact: Impact?) : Double {
        if (selectionStrategy != SubsetGeneSelectionStrategy.ADAPTIVE_WEIGHT) return defaultProbabilityToModifySize()
        impact?:return  defaultProbabilityToModifySize()
        if (impact !is CollectionImpact) return  defaultProbabilityToModifySize()

        return if (impact.recentImprovementOnSize()) defaultProbabilityToModifySize() * timesProbToModifySize() else defaultProbabilityToModifySize()
    }

    private fun timesProbToModifySize() : Int = 3

    /**
     * clear all elements
     */
    fun clearElements()

    /**
     * @return if [this] collection is empty, ie, elements is empty
     */
    fun isEmpty() : Boolean

}