package org.evomaster.core.search.gene.sql

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.interfaces.ComparableGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 *  https://www.postgresql.org/docs/14/rangetypes.html
 *  A representation of numeric range type.
 */
class SqlRangeGene<T>(
        /**
         * The name of this gene
         */
        name: String,

        private val template: T,

        private val isLeftClosed: BooleanGene = BooleanGene("isLeftClosed"),

        private val left: T = template.copy() as T,

        private val right: T = template.copy() as T,

        private val isRightClosed: BooleanGene = BooleanGene("isRightClosed")

) : CompositeFixedGene(name, mutableListOf(isLeftClosed, left, right, isRightClosed))
        where T : ComparableGene, T: Gene {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlRangeGene::class.java)
    }

    init {
        left.name = "left"
        right.name = "right"
        repairGeneIfNeeded()
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return left <= right
    }

    private fun swapLeftRightValues() {
        val copyOfLeftGene = left.copy()
        left.copyValueFrom(right)
        right.copyValueFrom(copyOfLeftGene)
    }

    /**
     * LowerBound must always be less than or equal
     * to UpperBound
     */
    private fun repairGeneIfNeeded() {
        if (left > right) {
            swapLeftRightValues()
        }
        assert(left <= right)
    }

    override fun mutationCheck(): Boolean {
        return isLocallyValid()
    }

    override fun repair() {
        repairGeneIfNeeded()
    }

    override fun copyContent(): Gene {
        return SqlRangeGene<T>(
                name = name,
                template = template.copy() as T,
                isLeftClosed = isLeftClosed.copy() as BooleanGene,
                left = left.copy() as T,
                right = right.copy() as T,
                isRightClosed = isRightClosed.copy() as BooleanGene
        )
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is SqlRangeGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        return updateValueOnlyIfValid(
            {isLeftClosed.copyValueFrom(other.isLeftClosed) &&
                    left.copyValueFrom(other.left as Gene) &&
                    right.copyValueFrom(other.right as Gene) &&
                    isRightClosed.copyValueFrom(other.isRightClosed)}, true
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlRangeGene<*>) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return isLeftClosed.containsSameValueAs(other.isRightClosed)
                && left.containsSameValueAs(other.left as Gene)
                && right.containsSameValueAs(other.right as Gene)
                && isRightClosed.containsSameValueAs(other.isRightClosed)
    }


    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        log.trace("Randomizing SqlRangeGene")
        listOf(isRightClosed, left, right, isRightClosed)
                .forEach { it.randomize(randomness, tryToForceNewValue) }
        repairGeneIfNeeded()
    }



    private fun isLeftOpen(): Boolean {
        return !isLeftClosed.value
    }

    private fun isRightOpen(): Boolean {
        return !isRightClosed.value
    }

    private fun isEmpty(): Boolean {
        return (isLeftOpen() || isRightOpen()) &&
                left.containsSameValueAs(right)
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


    override fun setValueBasedOn(gene: Gene): Boolean {
        if (gene is SqlRangeGene<*> && gene.template::class.java.simpleName == template::class.java.simpleName) {
            this.isLeftClosed.setValueBasedOn(gene.isLeftClosed)
            this.left.setValueBasedOn(gene.left as Gene)
            this.right.setValueBasedOn(gene.right as Gene)
            this.isRightClosed.setValueBasedOn(gene.isRightClosed)
        }
        LoggingUtil.uniqueWarn(
                log,
                "cannot bind SqlNumericRangeGene with the template (${template::class.java.simpleName}) with ${gene::class.java.simpleName}"
        )
        return false
    }

    override fun customShouldApplyShallowMutation(
        randomness: Randomness,
        selectionStrategy: SubsetGeneMutationSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): Boolean {
        return false
    }


}