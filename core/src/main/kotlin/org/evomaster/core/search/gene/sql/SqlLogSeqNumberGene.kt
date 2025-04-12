package org.evomaster.core.search.gene.sql

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.gene.numeric.LongGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.math.pow


/**
 *  Gene for postgres pg_lsn data type. This type can be used to store LSN (Log Sequence Number).
 *  Internally, an LSN is a 64-bit integer, representing a byte position in the write-ahead log stream.
 *  It is printed as two hexadecimal numbers of up to 8 digits each, separated by a slash;
 *  for example, 16/B374D848.
 *  Min value is 0/0
 *  Max value is FFFFFFFF/FFFFFFFF
 */
class SqlLogSeqNumberGene(
    /**
         * The name of this gene
         */
        name: String,

    /**
         * The left part of 32 bits
         */
        val leftPart: LongGene = LongGene("leftPart",
                value = 0,
                min = 0,
                max = MAX_VALUE,
                minInclusive = true,
                maxInclusive = true),

    /**
         * The right part of 32 bits
         */
        val rightPart: LongGene = LongGene("rightPart",
                value = 0,
                min = 0,
                max = MAX_VALUE,
                minInclusive = true,
                maxInclusive = true),

    ) : CompositeFixedGene(name, mutableListOf(leftPart, rightPart)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlLogSeqNumberGene::class.java)

        val MAX_VALUE = 2.toDouble().pow(32.toDouble()).toLong() - 1

        private fun toHex(value: Long) = value.toString(16)

    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun copyContent(): Gene {
        return SqlLogSeqNumberGene(
                name = name,
                leftPart = leftPart.copy() as LongGene,
                rightPart = rightPart.copy() as LongGene
        )
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is SqlLogSeqNumberGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return updateValueOnlyIfValid(
            {leftPart.copyValueFrom(other.leftPart)
                    && rightPart.copyValueFrom(other.rightPart)}, true
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlLogSeqNumberGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return leftPart.containsSameValueAs(other.leftPart)
                && rightPart.containsSameValueAs(other.rightPart)

    }


    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        log.trace("Randomizing ${this::class.java.simpleName}")
        val genes: List<Gene> = listOf(leftPart, rightPart)
        val index = randomness.nextInt(genes.size)
        genes[index].randomize(randomness, tryToForceNewValue)
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return String.format(
                "\"%s/%s\"",
                toHex(leftPart.value).uppercase(),
                toHex(rightPart.value).uppercase()
        )
    }


    override fun setValueBasedOn(gene: Gene): Boolean {
        if (gene is SqlLogSeqNumberGene) {
            this.leftPart.setValueBasedOn(gene.leftPart)
            this.rightPart.setValueBasedOn(gene.rightPart)
        }
        LoggingUtil.uniqueWarn(
                log,
                "cannot bind ${this::class.java.simpleName} with ${gene::class.java.simpleName}"
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