package org.evomaster.core.search.gene.sql

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 *  https://www.postgresql.org/docs/14/rangetypes.html
 *  A representation of numeric range type.
 */
class SqlNumericRangeGene<T>(
    /**
     * The name of this gene
     */
    name: String,

    private val template: NumberGene<T>,

    private val isLeftClosed: BooleanGene = BooleanGene("isLeftClosed"),

    private val left: NumberGene<T> = template.copyContent() as NumberGene<T>,

    private val right: NumberGene<T> = template.copyContent() as NumberGene<T>,

    private val isRightClosed: BooleanGene = BooleanGene("isRightClosed")

) : Gene(name, mutableListOf(isLeftClosed, left, right, isRightClosed))
        where T : Number {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlNumericRangeGene::class.java)
    }

    init {
        left.name = "left"
        right.name = "right"

        if (!isLeftLessThanEqualToRight()) {
            swapLeftRightValues()
        }

        assert(isLeftLessThanEqualToRight())
    }

    /**
     * LowerBound must always be less than or equal
     * to UpperBound
     */
    private fun isLeftLessThanEqualToRight(): Boolean {
        return left.value.toDouble() <= right.value.toDouble()
    }

    private fun swapLeftRightValues() {
        val previousLeftValue = left.value
        left.value = right.value
        right.value = previousLeftValue
    }

    override fun getChildren(): MutableList<Gene> =
        mutableListOf(isLeftClosed, left, right, isRightClosed)

    override fun copyContent(): Gene {
        return SqlNumericRangeGene<T>(
            name = name,
            template = template.copyContent() as NumberGene<T>,
            isLeftClosed = isLeftClosed.copyContent() as BooleanGene,
            left = left.copyContent() as NumberGene<T>,
            right = right.copyContent() as NumberGene<T>,
            isRightClosed = isRightClosed.copyContent() as BooleanGene
        )
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlNumericRangeGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        isRightClosed.value = other.isLeftClosed.value
        left.value = other.left.value as T
        right.value = other.right.value as T
        isRightClosed.value = other.isRightClosed.value
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlNumericRangeGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return isRightClosed.value.equals(other.isRightClosed.value)
                && left.value.equals(other.left.value)
                && right.value.equals(other.right.value)
                && isRightClosed.value.equals(other.isRightClosed.value)
    }


    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        log.trace("Randomizing SqlNumericRangeGene")
        val genes: List<Gene> = listOf(isRightClosed, left, right, isRightClosed)
        val index = randomness.nextInt(genes.size)
        genes[index].randomize(randomness, forceNewValue, allGenes)

        if (!isLeftLessThanEqualToRight()) {
            swapLeftRightValues()
        }
    }

    override fun candidatesInternalGenes(
        randomness: Randomness,
        apc: AdaptiveParameterControl,
        allGenes: List<Gene>,
        selectionStrategy: SubsetGeneSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): List<Gene> {
        return listOf(isLeftClosed, left, right, isRightClosed)
    }

    private fun isLeftOpen(): Boolean {
        return !isLeftClosed.value
    }

    private fun isRightOpen(): Boolean {
        return !isRightClosed.value
    }

    private fun isEmpty(): Boolean {
        return (isLeftOpen() || isRightOpen()) &&
                left.value.equals(right.value)
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        if (isEmpty())
            return "\"empty\""
        else
            return String.format(
                "\"%s %s , %s %s\"",
                if (isRightOpen()) '(' else '[',
                left.getValueAsRawString(),
                right.getValueAsRawString(),
                if (isLeftOpen()) ')' else ']'
            )
    }


    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(isLeftClosed)
                .plus(left)
                .plus(right)
                .plus(isRightClosed)
    }


    override fun innerGene(): List<Gene> =
        listOf(isLeftClosed, left, right, isRightClosed)

    override fun bindValueBasedOn(gene: Gene): Boolean {
        if (gene is SqlNumericRangeGene<*> && gene.template::class.java.simpleName == template::class.java.simpleName) {
            this.isLeftClosed.bindValueBasedOn(gene.isLeftClosed)
            this.left.bindValueBasedOn(gene.left)
            this.right.bindValueBasedOn(gene.right)
            this.isRightClosed.bindValueBasedOn(gene.isRightClosed)
        }
        LoggingUtil.uniqueWarn(
            log,
            "cannot bind SqlNumericRangeGene with the template (${template::class.java.simpleName}) with ${gene::class.java.simpleName}"
        )
        return false
    }


}