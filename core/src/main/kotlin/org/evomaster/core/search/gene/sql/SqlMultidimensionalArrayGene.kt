package org.evomaster.core.search.gene.sql

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.impact.impactinfocollection.CollectionImpact
import org.evomaster.core.search.impact.impactinfocollection.Impact
import org.evomaster.core.search.impact.impactinfocollection.ImpactUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * https://www.postgresql.org/docs/14/arrays.html
 * A multidimensional array representation.
 * When created, all dimensions have length 0
 */
class SqlMultidimensionalArrayGene<T>(
        /**
         * The name of this gene
         */
        name: String,
        /**
         * The type for this array. Every time we create a new element to add, it has to be based
         * on this template
         */
        val template: T,
        /**
         * Fixed number of dimensions for this multidimensional array.
         * Once created, the number of dimensions do not change.
         */
        val numberOfDimensions: Int,
        /**
         *  How many elements each dimension can have.
         */
        private val maxDimensionSize: Int = ArrayGene.MAX_SIZE,

        /**
         * The multidimensional array is internally represented
         * as nested lists of genes. For example, a mutidimensional
         * array with only one dimension (i.e. an array) is a
         * list of genes:
         * { g1, g2, g3 }
         * On the other hand, a matrix is a list of lists of genes.
         * Each list of the same size:
         * { {g1,g2,g3} , {g4, g5, g6} }
         * Similarly, a three dimensional array is a list of lists of lists of
         * genes:
         * { { {g1,g2,g3} , {g4, g5, g6} }, { {g7,g8,g9} , {g10, g11, g12} } }.
         *
         * Multidimensional arrays are inialized with no elements (i.e.
         * the length of each dimension is 0).
         */
        private val nestedListOfElements: MutableList<Any> = mutableListOf()
) : CollectionGene, CompositeGene(name, mutableListOf()) where T : Gene {

    init {
        if (numberOfDimensions < 0)
            throw IllegalArgumentException("Invalid number of dimensions ${numberOfDimensions}")
    }

    companion object {

        const val DEFAULT_MIN_SIZE = 0

        val log: Logger = LoggerFactory.getLogger(SqlMultidimensionalArrayGene::class.java)

        /**
         * Checks if the given index is within range.
         * Otherwise an IndexOutOfBounds exception is thrown.
         */
        private fun checkIndexWithinRange(index: Int, indices: IntRange) {
            if (index !in indices) {
                throw IndexOutOfBoundsException("Cannot access index ${index} in a dimension of size ${indices}")
            }
        }

        /**
         * Returns a list with all the genes contained in the
         * internal representation of the multidimensional array.
         */
        private fun getAllGenes(nestedListOfElements: List<Any>): List<Gene> {
            val result = mutableListOf<Gene>()
            nestedListOfElements.forEach {
                when (it) {
                    is Gene -> result.add(it)
                    is List<*> -> result.addAll(getAllGenes(it as List<Any>))
                }
            }
            return result
        }

        /**
         * Recursively creates the internal representation
         * of the multidimensional array. The argument is
         * a list of the size for each dimension.
         */
        private fun <T : Gene> buildNewElements(
                dimensionSizes: List<Int>,
                template: T
        ): MutableList<Any> {
            val currentDimensionSize = dimensionSizes.first()
            val nextDimensionSizes = dimensionSizes.drop(1)
            val nestedListOfNewElements: MutableList<Any> = mutableListOf()
            repeat(currentDimensionSize) {
                val element = if (nextDimensionSizes.isEmpty()) {
                    template.copy()
                } else {
                    buildNewElements(nextDimensionSizes, template)
                }
                nestedListOfNewElements.add(element)
            }
            return nestedListOfNewElements
        }

        private fun containsSameValueAs(nestedListOfElements: List<Any>, otherNestedListOfElements: List<Any>): Boolean {
            if (nestedListOfElements.size != otherNestedListOfElements.size)
                return false

            return nestedListOfElements.zip(otherNestedListOfElements) { a, b ->
                when (a) {
                    is Gene -> a.containsSameValueAs(b as Gene)
                    is List<*> -> containsSameValueAs(a as List<Any>, b as List<Any>)
                    else -> throw IllegalArgumentException("Unsupported class type ${a.javaClass}")
                }
            }.all { it }
        }

        private fun copyValueFrom(otherNestedListOfElements: List<Any>): List<Any> {
            return otherNestedListOfElements.map { e ->
                when (e) {
                    is Gene -> e.copyContent()
                    is List<*> -> copyValueFrom(e as List<Any>)
                    else -> throw IllegalArgumentException("Unsupported class type ${e.javaClass}")
                }
            }.toMutableList()
        }

        private fun getValueAsPrintableString(
                nestedListOfElements: List<Any>,
                previousGenes: List<Gene>,
                mode: GeneUtils.EscapeMode?,
                targetFormat: OutputFormat?,
                extraCheck: Boolean
        ): String {
            return nestedListOfElements.joinToString(",") { e ->
                when (e) {
                    is Gene ->
                        e.getValueAsPrintableString(previousGenes, mode = GeneUtils.EscapeMode.TEXT, targetFormat = OutputFormat.DEFAULT, extraCheck)
                    is List<*> -> "{${
                        getValueAsPrintableString(
                                e as List<Any>,
                                previousGenes,
                                mode,
                                targetFormat,
                                extraCheck
                        )
                    }}"
                    else -> throw IllegalArgumentException("Unsupported class type ${e.javaClass}")
                }
            }
        }
    }

    /**
     * Returns the element by using a list of indices for
     * each dimension. The number of indices must be equal
     * to the number of dimensions of the multidimensional array.
     * Additionally, each index must be within range.
     *
     * For example, given the following multidimensional array.
     *  { {g1,g2,g3} , {g4, g5, g6} }
     * getElement({0,0}) returns g1.
     * getElement({2,2}) returns g6
     */
    fun getElement(dimensionIndexes: List<Int>): Gene {
        if (dimensionIndexes.size != numberOfDimensions) {
            throw IllegalArgumentException("Incorrect number of indices to get an element of an array of ${numberOfDimensions} dimensions")
        }

        var currentNestedListOfElements = nestedListOfElements
        ((0..(dimensionIndexes.size - 2))).forEach {
            checkIndexWithinRange(dimensionIndexes[it], currentNestedListOfElements.indices)
            currentNestedListOfElements = currentNestedListOfElements[dimensionIndexes[it]] as MutableList<Any>
        }
        checkIndexWithinRange(dimensionIndexes.last(), currentNestedListOfElements.indices)
        return currentNestedListOfElements[dimensionIndexes.last()] as Gene
    }

    /**
     * Returns the dimension size for the required dimension
     * For example, given the following multidimensional array.
     *  { {g1,g2,g3} , {g4, g5, g6} }
     * getDimensionSize(0) returns 2.
     * getDimensionSize(1) returns 3
     */
    fun getDimensionSize(dimensionIndex: Int): Int {
        var currentNestedListOfElements = nestedListOfElements
        if (nestedListOfElements.isEmpty()) {
            checkIndexWithinRange(dimensionIndex, IntRange(0, numberOfDimensions - 1))
            return 0
        }


        repeat(dimensionIndex) {
            currentNestedListOfElements = currentNestedListOfElements[0] as MutableList<Any>
        }
        return currentNestedListOfElements.size
    }


    /**
     * Randomizes the whole multidimensional array by removing all dimensions, and then
     * creating new sizes for each dimension, and new gene elements from the template.
     */
    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        // get the size of each dimension
        val dimensionSizes: MutableList<Int> = buildNewDimensionSizes(randomness)

        replaceElements(dimensionSizes)
        // randomize fresh elements
        getAllGenes(nestedListOfElements).forEach { it.randomize(randomness, forceNewValue, allGenes) }
    }

    /**
     * Creates a new list of randomized dimension sizes.
     * The total number of dimensions is equal to numberOfDimensions, and
     * no dimension is greater than maxDimensionSize, or less than 1.
     */
    private fun buildNewDimensionSizes(randomness: Randomness): MutableList<Int> {
        val dimensionSizes: MutableList<Int> = mutableListOf()
        repeat(numberOfDimensions) {
            val dimensionSize = randomness.nextInt(1, maxDimensionSize)
            dimensionSizes.add(dimensionSize)
        }
        return dimensionSizes
    }





    /**
     * Returns true if the other gene is another multidimensional array,
     * with the same number of dimensions, size of each dimension,
     * and containsSameValueAs the other genes.
     */
    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlMultidimensionalArrayGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        if (this.numberOfDimensions != other.numberOfDimensions)
            return false
        return containsSameValueAs(this.nestedListOfElements, other.nestedListOfElements)
    }

    override fun innerGene(): List<Gene> {
        return getAllGenes(nestedListOfElements)
    }

    override fun bindValueBasedOn(gene: Gene): Boolean {
        if ((gene !is SqlMultidimensionalArrayGene<*>)
                || (gene.template::class.java.simpleName != template::class.java.simpleName)
        ) {
            LoggingUtil.uniqueWarn(
                    log,
                    "cannot bind SqlMultidimensionalArrayGene with the template (${template::class.java.simpleName}) with ${gene::class.java.simpleName}"
            )
            return false
        }

        if (numberOfDimensions != gene.numberOfDimensions) {
            LoggingUtil.uniqueWarn(
                    log,
                    "cannot bind SqlMultidimensionalArrayGene of ${numberOfDimensions} dimensions to gene of ${gene.numberOfDimensions}"
            )
            return false
        }
        this.copyValueFrom(gene)
        return true
    }

//    override fun getChildren(): List<out StructuralElement> {
//        return getAllGenes(nestedListOfElements)
//        //TODO discuss
//    }

    override fun getValueAsPrintableString(
            previousGenes: List<Gene>,
            mode: GeneUtils.EscapeMode?,
            targetFormat: OutputFormat?,
            extraCheck: Boolean
    ): String {
        return "\"{${getValueAsPrintableString(this.nestedListOfElements, previousGenes, mode, targetFormat, extraCheck)}}\""
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlMultidimensionalArrayGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        if (numberOfDimensions != other.numberOfDimensions) {
            throw IllegalArgumentException("Cannot copy value to array of  ${numberOfDimensions} dimensions from array of ${other.numberOfDimensions} dimensions")
        }

        // clear elements (remove from its binding genes if needed)
        clearElements()
        addAllElements(copyValueFrom(other.nestedListOfElements))
    }

    fun replaceElements(
            dimensionSizes: List<Int>
    ) {
        // check the number of dimensions is correct
        if (dimensionSizes.size != this.numberOfDimensions) {
            throw IllegalArgumentException("Cannot create an multidimensional array of ${numberOfDimensions} with a list of ${dimensionSizes.size}")
        }
        // check each dimension size is greater or equal to 0
        if (dimensionSizes.any { it < 0 })
            throw IllegalArgumentException(
                    "Cannot create a dimension of size ${
                        dimensionSizes.first { it < 0 }
                    }"
            )

        // remove old elements (remove from its binding genes if needed)
        clearElements()
        // build new elements using template with the internal representation
        val newElements = buildNewElements(dimensionSizes, template)
        // store new freshly created elements
        addAllElements(newElements)
    }

    /**
     * Add all elements (adding them to the structured gene)
     */
    private fun addAllElements(newElements: List<Any>) {
        nestedListOfElements.addAll(newElements)
        addChildren(getAllGenes(nestedListOfElements))
    }

    /**
     * Remove all elements (and clear genes from its binding genes if needed)
     */
    override fun clearElements() {
        getAllGenes(nestedListOfElements).forEach { it.removeThisFromItsBindingGenes() }
        nestedListOfElements.clear()
    }

    override fun isEmpty() = children.isEmpty()

    override fun getSizeOfElements(filterMutable: Boolean): Int {
        val genes = getAllGenes(nestedListOfElements)
        if (!filterMutable)
            return genes.size
        else
            return genes.count { it.isMutable() }
    }

    override fun getGeneName() = name

    override fun getSpecifiedMinSize() = DEFAULT_MIN_SIZE

    override fun getMinSizeOrDefault() = DEFAULT_MIN_SIZE

    /**
     * For example, a multidimensional array built with 3 dimensions
     * has a default max size of 3 * ArrayGene.MAX_SIZE,
     * independently of the maxDimensionSize passed to the constructor.
     */
    override fun getDefaultMaxSize() = numberOfDimensions * ArrayGene.MAX_SIZE

    /**
     * For example, a multidimensional array built with 3 and a
     * maxDimensionalSize of 10 has a specified max size of 3 * 10
     */
    override fun getSpecifiedMaxSize() = numberOfDimensions * maxDimensionSize

    /**
     * Returns the maxDimensionalSize * numberOfDimensions
     * By default, maxDimensionalSize is equal to ArrayGene.MAX_SIZE.
     */
    override fun getMaxSizeOrDefault() = getSpecifiedMaxSize()

    /**
     * 1 is for 'remove' or 'add' element.
     * The mutationWeight is computed on all elements in the same way
     * as ArrayGene.mutationWeight()
     */
    override fun mutationWeight(): Double {
        return 1.0 + getAllGenes(this.nestedListOfElements).sumOf { it.mutationWeight() }
    }


    /**
     * The function adaptiveSelectSubset() behaves as ArrayGene.adaptiveSelectSubset()
     */
    override fun adaptiveSelectSubset(randomness: Randomness,
                                      internalGenes: List<Gene>,
                                      mwc: MutationWeightControl,
                                      additionalGeneMutationInfo: AdditionalGeneMutationInfo
    ): List<Pair<Gene, AdditionalGeneMutationInfo?>> {
        /*
            element is dynamically modified, then we do not collect impacts for it now.
            thus for the internal genes, adaptive gene selection for mutation is not applicable
        */
        val s = randomness.choose(internalGenes)
        /*
            TODO impact for an element in ArrayGene
         */
        val geneImpact = ImpactUtils.createGeneImpact(s, s.name)
        return listOf(s to additionalGeneMutationInfo.copyFoInnerGene(geneImpact, s))
    }


    override fun candidatesInternalGenes(randomness: Randomness,
                                         apc: AdaptiveParameterControl,
                                         allGenes: List<Gene>,
                                         selectionStrategy: SubsetGeneSelectionStrategy,
                                         enableAdaptiveGeneMutation: Boolean,
                                         additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): List<Gene> {
        if (!isMutable()) {
            throw IllegalStateException("Cannot mutate a immutable multidimensional array")
        }
        val mutableGenes = getAllGenes(nestedListOfElements).filter { it.isMutable() }
        // if min == max, the size is not mutable
        if (getMinSizeOrDefault() == getMaxSizeOrDefault() && nestedListOfElements.size == getMinSizeOrDefault())
            return mutableGenes
        // if mutable is empty, modify size
        if (mutableGenes.isEmpty()) return listOf()

        val p = probabilityToModifySize(selectionStrategy, additionalGeneMutationInfo?.impact)
        return if (randomness.nextBoolean(p)) listOf() else mutableGenes
    }

    /**
     * an impact-based probability fo modifying size/dimensions of the multidimensional array
     */
    override fun probabilityToModifySize(selectionStrategy: SubsetGeneSelectionStrategy, impact: Impact?): Double {
        if (selectionStrategy != SubsetGeneSelectionStrategy.ADAPTIVE_WEIGHT) return defaultProbabilityToModifySize()
        impact ?: return defaultProbabilityToModifySize()
        if (impact !is CollectionImpact) return defaultProbabilityToModifySize()

        return if (impact.recentImprovementOnSize()) defaultProbabilityToModifySize() * timesProbToModifySize() else defaultProbabilityToModifySize()
    }

    /**
     * Same value as ArrayGene.timesProbToModifySize()
     */
    private fun timesProbToModifySize(): Int = 3

    override fun copyContent() = SqlMultidimensionalArrayGene(name = name,
            template = template.copyContent(),
            numberOfDimensions = numberOfDimensions,
            maxDimensionSize = maxDimensionSize,
            nestedListOfElements = copyValueFrom(nestedListOfElements).toMutableList())

    override fun mutate(
            randomness: Randomness,
            apc: AdaptiveParameterControl,
            mwc: MutationWeightControl,
            allGenes: List<Gene>,
            selectionStrategy: SubsetGeneSelectionStrategy,
            enableAdaptiveGeneMutation: Boolean,
            additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        this.randomize(randomness, true, allGenes)
        return true
    }

}