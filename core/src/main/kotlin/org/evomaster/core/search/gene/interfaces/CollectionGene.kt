package org.evomaster.core.search.gene.interfaces

import org.evomaster.core.search.gene.BooleanGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.collection.EnumGene
import org.evomaster.core.search.gene.numeric.IntegerGene
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.impact.impactinfocollection.CollectionImpact
import org.evomaster.core.search.impact.impactinfocollection.Impact
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import kotlin.math.min

/**
 * For genes representing typical collections like arrays, maps and sets.
 *
 * Note: a gene with mutable number of children is not necessarily a collection
 *
 * Note: if a gene has a child that is a collection, we do not necessarily need to mark the
 * parent as a collection
 *
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
    fun probabilityToModifySize(selectionStrategy: SubsetGeneMutationSelectionStrategy, impact: Impact?) : Double {
        if (selectionStrategy != SubsetGeneMutationSelectionStrategy.ADAPTIVE_WEIGHT) return defaultProbabilityToModifySize()
        impact?:return  defaultProbabilityToModifySize()
        if (impact !is CollectionImpact) return  defaultProbabilityToModifySize()

        return if (impact.recentImprovementOnSize()) defaultProbabilityToModifySize() * timesProbToModifySize() else defaultProbabilityToModifySize()
    }

    private fun timesProbToModifySize() : Int = 3


    /**
     * a list of types of gene for collection uniqueness check, eg, ArrayGene, and key of MapGene
     * currently, we only support the uniqueness check for Integer, String, LongGene, Enum and Boolean
     *
     * TODO for other types if needed
     */
    fun isElementApplicableToUniqueCheck(gene : Gene) : Boolean{
        return  gene is IntegerGene || gene is StringGene || gene is LongGene || gene is EnumGene<*> || gene is BooleanGene
    }

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
    fun getSizeOfElements(filterMutable: Boolean = false) : Int

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
     * the default MaxSize here is set in evomaster
     * for handling mutation and randomizing values (avoid handling huge amount elements)
     *
     * but the default maxsize might be modified based on the specified minSize
     */
    fun getDefaultMaxSize() : Int

}