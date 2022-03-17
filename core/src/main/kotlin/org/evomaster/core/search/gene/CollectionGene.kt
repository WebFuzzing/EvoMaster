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


    /**
     * @return max size of elements referred in the collection gene
     */
    fun getMaxSizeOrDefault() : Int

    /**
     * @return max size of elements allowed in the collection gene
     */
    fun getSpecifiedMaxSize() : Int?

    /**
     * @return min size of elements referred in the collection gene
     */
    fun getMinSizeOrDefault() : Int

    /**
     * @return min size of elements should exist in the collection gene
     */
    fun getSpecifiedMinSize() : Int?

    /**
     * @param filterMutable represents if only consider mutable genes
     * @return a size of elements existing in the collection gene
     */
    fun getSizeOfElements(filterMutable: Boolean) : Int

    fun getGeneName() : String

    fun checkConstraintsForAdd(){
        if (getSpecifiedMaxSize() == getSizeOfElements(false))
            throw IllegalStateException("maxSize is ${getMaxSizeOrDefault()} and minSize is ${getMinSizeOrDefault()} and sizeOfElements is ${getSizeOfElements(false)} (${getSizeOfElements(true)}), cannot add more elements for the gene ${getGeneName()}")
    }

    fun checkConstraintsForRemoval(){
        if (getSpecifiedMinSize() == getSizeOfElements(false))
            throw IllegalStateException("maxSize is ${getMaxSizeOrDefault()} and minSize is ${getMinSizeOrDefault()} and sizeOfElements is ${getSizeOfElements(false)} (${getSizeOfElements(true)}), cannot remove any element for the gene ${getGeneName()}")
    }

    fun checkConstraintsForValueMutation(){
        if (getSizeOfElements(true) == 0)
            throw IllegalStateException("${getSizeOfElements(true)} is 0, cannot mutate any element in the gene ${getGeneName()}")
    }

}