package org.evomaster.core.search.gene.sql

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.ArrayGene
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.Randomness
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
     * Fixed number of dimensions for this multidimensional array
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
    private val elements: MutableList<Any> = mutableListOf()
) : Gene(name, listOf()) where T : Gene {

    init {
        if (numberOfDimensions < 0)
            throw IllegalArgumentException("Invalid number of dimensions ${numberOfDimensions}")
    }

    companion object {

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
        private fun getAllGenes(listOfElements: List<Any>): List<Gene> {
            val result = mutableListOf<Gene>()
            listOfElements.forEach {
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
            val list: MutableList<Any> = mutableListOf()
            repeat(currentDimensionSize) {
                val element = if (nextDimensionSizes.isEmpty()) {
                    template.copy()
                } else {
                    buildNewElements(nextDimensionSizes, template)
                }
                list.add(element)
            }
            return list
        }

        private fun containsSameValueAs(thisElements: List<Any>, otherElements: List<Any>): Boolean {
            if (thisElements.size != otherElements.size)
                return false

            return thisElements.zip(otherElements) { a, b ->
                when (a) {
                    is Gene -> a.containsSameValueAs(b as Gene)
                    is List<*> -> containsSameValueAs(a as List<Any>, b as List<Any>)
                    else -> throw IllegalArgumentException("Unsupported class type ${a.javaClass}")
                }
            }.all { it }
        }

        private fun copyValueFrom(otherElements: List<Any>): List<Any> {
            return otherElements.map { e ->
                when (e) {
                    is Gene -> e.copyContent()
                    is List<*> -> copyValueFrom(e as List<Any>)
                    else -> throw IllegalArgumentException("Unsupported class type ${e.javaClass}")
                }
            }.toMutableList()
        }

        private fun getValueAsPrintableString(
            elements: List<Any>,
            previousGenes: List<Gene>,
            mode: GeneUtils.EscapeMode?,
            targetFormat: OutputFormat?,
            extraCheck: Boolean
        ): String {
            return elements.joinToString(",") { e ->
                when (e) {
                    is Gene -> e.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck)
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

        var list = elements
        ((0..(dimensionIndexes.size - 2))).forEach {
            checkIndexWithinRange(dimensionIndexes[it], list.indices)
            list = list[dimensionIndexes[it]] as MutableList<Any>
        }
        checkIndexWithinRange(dimensionIndexes.last(), list.indices)
        return list[dimensionIndexes.last()] as Gene
    }

    /**
     * Returns the dimension size for the required dimension
     * For example, given the following multidimensional array.
     *  { {g1,g2,g3} , {g4, g5, g6} }
     * getDimensionSize(0) returns 2.
     * getDimensionSize(1) returns 3
     */
    fun getDimensionSize(dimensionIndex: Int): Int {
        var list = elements
        if (elements.isEmpty()) {
            checkIndexWithinRange(dimensionIndex, IntRange(0, numberOfDimensions - 1))
            return 0
        }


        repeat(dimensionIndex) {
            list = list[0] as MutableList<Any>
        }
        return list.size
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
        getAllGenes(elements).forEach { it.randomize(randomness, forceNewValue, allGenes) }
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


    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(getAllGenes(elements).flatMap { g -> g.flatView(excludePredicate) })
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
        return containsSameValueAs(this.elements, other.elements)
    }

    override fun innerGene(): List<Gene> {
        return getAllGenes(elements)
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

    override fun getChildren(): List<out StructuralElement> {
        return getAllGenes(elements)
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return "\"{${getValueAsPrintableString(this.elements, previousGenes, mode, targetFormat, extraCheck)}}\""
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
        addAllElements(copyValueFrom(other.elements))
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
        elements.addAll(newElements)
        addChildren(getAllGenes(elements))
    }

    /**
     * Remove all elements (and clear genes from its binding genes if needed)
     */
    private fun clearElements() {
        getAllGenes(elements).forEach { it.removeThisFromItsBindingGenes() }
        elements.clear()
    }

}