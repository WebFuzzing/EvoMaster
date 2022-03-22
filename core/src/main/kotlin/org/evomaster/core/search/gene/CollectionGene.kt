package org.evomaster.core.search.gene

import org.evomaster.core.search.impact.impactinfocollection.CollectionImpact
import org.evomaster.core.search.impact.impactinfocollection.Impact
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import kotlin.math.min

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
     *
     * a max size could be huge by default in the schema, eg, 2147483647
     * https://javaee.github.io/javaee-spec/javadocs/javax/validation/constraints/Size.html#max--
     * to avoid such huge number of elements in collection used in further mutation or randomization
     * @return a max size used in randomizing size
     */
    fun getMaxSizeUsedInRandomize() : Int {
        return min(getDefaultMaxSize(), getMaxSizeOrDefault())
    }

    /**
     * @return the max size configured in the schema or the default one in evomaster
     */
    fun getMaxSizeOrDefault() : Int

    /**
     * @return the min size configured in the schema or the default one in evomaster
     */
    fun getMinSizeOrDefault() : Int

    /**
     * the default MaxSize here is set in evomaster
     * for handling mutation and randomizing values (avoid handling huge amount elements)
     *
     * but the default maxsize might be modified based on the specified minSize
     */
    fun getDefaultMaxSize() : Int

}