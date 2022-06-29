package org.evomaster.core.search.gene.sql

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.*
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
        private val maxDimensionSize: Int = ArrayGene.MAX_SIZE
) : CompositeGene(name, mutableListOf()) where T : Gene {

    /**
     * Stores what is the size of each dimension.
     * It is used to check that the multidimensional array
     * is valid.
     */
    private var dimensionSizes: List<Int> = listOf()

    init {
        if (numberOfDimensions < 1)
            throw IllegalArgumentException("Invalid number of dimensions $numberOfDimensions")
    }

    /*
        FIXME
        only one direct child
        override children modifications to rather work on internal array genes.

        but, before that, need to refactor/look-into CollectionGene
     */

    companion object {

        val log: Logger = LoggerFactory.getLogger(SqlMultidimensionalArrayGene::class.java)

        /**
         * Checks if the given index is within range.
         * Otherwise an IndexOutOfBounds exception is thrown.
         */
        private fun checkIndexWithinRange(index: Int, indices: IntRange) {
            if (index !in indices) {
                throw IndexOutOfBoundsException("Cannot access index $index in a dimension of size $indices")
            }
        }


        /**
         * Recursively creates the internal representation
         * of the multidimensional array. The argument is
         * a list of the size for each dimension.
         */
        private fun <T : Gene> buildNewElements(
                dimensionSizes: List<Int>,
                elementTemplate: T
        ): ArrayGene<*> {
            val s = dimensionSizes[0]
            return if (dimensionSizes.size == 1) {
                // leaf ArrayGene case
                ArrayGene("[$s]", elementTemplate.copy(), maxSize = s, minSize = s, openingTag = "{", closingTag = "}", separatorTag = ",")
            } else {
                // nested/inner ArrayGene case
                val currentDimensionSize = dimensionSizes.first()
                val nextDimensionSizes = dimensionSizes.drop(1)
                val arrayTemplate = buildNewElements(nextDimensionSizes, elementTemplate)

                ArrayGene("[$currentDimensionSize]${arrayTemplate.name}", arrayTemplate,
                        maxSize = currentDimensionSize, minSize = currentDimensionSize, openingTag = "{", closingTag = "}", separatorTag = ",")
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
    fun getElement(dimensionIndexes: List<Int>): T {
        if (!initialized) {
            throw IllegalStateException("Cannot get element from an unitialized multidimensional array")
        }

        if (dimensionIndexes.size != numberOfDimensions) {
            throw IllegalArgumentException("Incorrect number of indices to get an element of an array of $numberOfDimensions dimensions")
        }

        var current = getArray()
        ((0..(dimensionIndexes.size - 2))).forEach {
            checkIndexWithinRange(dimensionIndexes[it], current.getViewOfElements().indices)
            current = current.getViewOfElements()[dimensionIndexes[it]] as ArrayGene<*>
        }
        checkIndexWithinRange(dimensionIndexes.last(), current.getViewOfElements().indices)
        return current.getViewOfElements()[dimensionIndexes.last()] as T
    }

    private fun isValid(currentArrayGene: ArrayGene<*>, currentDimensionIndex: Int): Boolean {
        return if (!currentArrayGene.isLocallyValid())
            false
        else if (currentArrayGene.getViewOfChildren().size != this.dimensionSizes[currentDimensionIndex]) {
            false
        } else if (currentDimensionIndex == numberOfDimensions - 1) {
            // case of leaf ArrayGene
            currentArrayGene.getViewOfChildren().all { it::class.java.isAssignableFrom(template::class.java) }
        } else {
            // case of inner, nested ArrayGene
            currentArrayGene.getViewOfChildren().all { isValid(it as ArrayGene<*>, currentDimensionIndex + 1) }
        }
    }

    /**
     * Check that the nested arraygenes equal the number of dimensions, check that each
     * dimension length is preserved.
     */
    override fun isLocallyValid(): Boolean {
        return if (this.children.size != 1) {
            false
        } else {
            isValid(this.children[0] as ArrayGene<*>, 0)
        }
    }

    /**
     *  Requires the multidimensional array to be initialized
     */
    private fun getArray(): ArrayGene<*> {
        assert(initialized)
        return children[0] as ArrayGene<*>
    }

    /**
     * Returns the dimension size for the required dimension
     * For example, given the following multidimensional array.
     *  { {g1,g2,g3} , {g4, g5, g6} }
     * getDimensionSize(0) returns 2.
     * getDimensionSize(1) returns 3
     */
    fun getDimensionSize(dimensionIndex: Int): Int {
        if (!initialized) {
            throw IllegalStateException("Cannot get element from an unitialized multidimensional array")
        }

        if (dimensionIndex >= this.numberOfDimensions) {
            throw IndexOutOfBoundsException("Cannot get dimension size of dimension ${dimensionIndex} for an array of ${numberOfDimensions} dimensions")
        }

        var current = getArray()
        if (current.getViewOfElements().isEmpty()) {
            checkIndexWithinRange(dimensionIndex, IntRange(0, numberOfDimensions - 1))
            return 0
        }

        repeat(dimensionIndex) {
            current = current.getViewOfElements()[0] as ArrayGene<*>
        }
        return current.getViewOfElements().size
    }


    /**
     * Randomizes the whole multidimensional array by removing all dimensions, and then
     * creating new sizes for each dimension, and new gene elements from the template.
     */
    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean, allGenes: List<Gene>) {
        val newDimensionSizes: List<Int> = buildNewDimensionSizes(randomness)
        val newChild = buildNewElements(newDimensionSizes, template.copy())

        killAllChildren()
        addChild(newChild)
        this.dimensionSizes = newDimensionSizes
        newChild.randomize(randomness, tryToForceNewValue, allGenes)
    }

    /**
     * Creates a new list of randomized dimension sizes.
     * The total number of dimensions is equal to numberOfDimensions, and
     * no dimension is greater than maxDimensionSize.
     * If the array is unidimensional, the dimensionSize could be 0, otherwise
     * is always greater than 0.
     */
    private fun buildNewDimensionSizes(randomness: Randomness): List<Int> {
        if (numberOfDimensions == 1) {
            val dimensionSize = randomness.nextInt(0, maxDimensionSize)
            return mutableListOf(dimensionSize)
        } else {
            val dimensionSizes: MutableList<Int> = mutableListOf()
            repeat(numberOfDimensions) {
                val dimensionSize = randomness.nextInt(1, maxDimensionSize)
                dimensionSizes.add(dimensionSize)
            }
            return dimensionSizes.toList()
        }
    }


    /**
     * Returns true if the other gene is another multidimensional array,
     * with the same number of dimensions, size of each dimension,
     * and containsSameValueAs the other genes.
     */
    override fun containsSameValueAs(other: Gene): Boolean {
        if (!initialized) {
            throw IllegalStateException("Cannot call to containsSameValueAs using an unitialized multidimensional array")
        }
        if (other !is SqlMultidimensionalArrayGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        if (this.numberOfDimensions != other.numberOfDimensions) {
            return false
        }
        if (this.dimensionSizes != other.dimensionSizes) {
            return false
        }
        return this.getViewOfChildren()[0].containsSameValueAs(other.getViewOfChildren()[0])
    }

    override fun innerGene(): List<Gene> {
        return listOf(getArray())
    }

    /**
     * A multidimensional array gene can only bind to other multidimensional array genes
     * with the same template and number of dimensions.
     */
    override fun bindValueBasedOn(gene: Gene): Boolean {
        if (gene !is SqlMultidimensionalArrayGene<*>) {
            LoggingUtil.uniqueWarn(ArrayGene.log, "cannot bind SqlMultidimensionalArrayGene to ${gene::class.java.simpleName}")
            return false
        }
        if (gene.template::class.java.simpleName != template::class.java.simpleName) {
            LoggingUtil.uniqueWarn(ArrayGene.log, "cannot bind SqlMultidimensionalArrayGene with the template (${template::class.java.simpleName}) with ${gene::class.java.simpleName}")
            return false
        }
        if (numberOfDimensions != gene.numberOfDimensions) {
            LoggingUtil.uniqueWarn(ArrayGene.log, "cannot bind SqlMultidimensionalArrayGene of ${numberOfDimensions} dimensions to another multidimensional array gene of ${gene.numberOfDimensions}")
            return false
        }
        killAllChildren()
        val elements = gene.getViewOfChildren().mapNotNull { it.copy() as? T }.toMutableList()
        addChildren(elements)
        this.dimensionSizes = gene.dimensionSizes
        return true
    }

    override fun getValueAsPrintableString(
            previousGenes: List<Gene>,
            mode: GeneUtils.EscapeMode?,
            targetFormat: OutputFormat?,
            extraCheck: Boolean
    ): String {
        if (!initialized) {
            throw IllegalStateException("Cannot call to getValueAsPrintableString() using an unitialized multidimensional array")
        }
        return "\"${this.children[0].getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)}\""
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlMultidimensionalArrayGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        if (numberOfDimensions != other.numberOfDimensions) {
            throw IllegalArgumentException("Cannot copy value to array of  $numberOfDimensions dimensions from array of ${other.numberOfDimensions} dimensions")
        }
        getViewOfChildren()[0].copyValueFrom(other.getViewOfChildren()[0])
        this.dimensionSizes = other.dimensionSizes
    }


    /**
     * 1 is for 'remove' or 'add' element.
     * The mutationWeight is computed on all elements in the same way
     * as ArrayGene.mutationWeight()
     */
    override fun mutationWeight(): Double {
        return 1.0 //TODO
        //return 1.0 + getAllGenes(this.nestedListOfElements).sumOf { it.mutationWeight() }
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
        //TODO
        return listOf()
    }


    override fun copyContent(): Gene {
        /*
            TODO Not sure about this. Even if we want to put this constraint, then should be in Gene
         */
//        if (!initialized) {
//            throw IllegalStateException("Cannot call to copyContent() from an uninitialized multidimensional array")
//        }

        val copy = SqlMultidimensionalArrayGene(
                name = name,
                template = template.copy(),
                numberOfDimensions = numberOfDimensions,
                maxDimensionSize = maxDimensionSize,
        )

        if (children.isNotEmpty()) {
            copy.addChild(this.children[0].copy())
        }
        copy.dimensionSizes = this.dimensionSizes
        return copy
    }

    override fun shallowMutate(
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