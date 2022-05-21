package org.evomaster.core.search.gene.sql

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
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
        private val leftPart: LongGene = LongGene("leftPart",
                value = 0,
                min = 0,
                max = MAX_VALUE,
                minInclusive = true,
                maxInclusive = true),

        /**
         * The right part of 32 bits
         */
        private val rightPart: LongGene = LongGene("rightPart",
                value = 0,
                min = 0,
                max = MAX_VALUE,
                minInclusive = true,
                maxInclusive = true),

        ) : Gene(name, mutableListOf(leftPart, rightPart)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlLogSeqNumberGene::class.java)

        val MAX_VALUE = 2.toDouble().pow(32.toDouble()).toLong() - 1

        private fun toHex(value: Long) = value.toString(16)

    }


    override fun copyContent(): Gene {
        return SqlLogSeqNumberGene(
                name = name,
                leftPart = leftPart.copyContent() as LongGene,
                rightPart = rightPart.copyContent() as LongGene
        )
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlLogSeqNumberGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        leftPart.copyValueFrom(other.leftPart)
        rightPart.copyValueFrom(other.rightPart)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlLogSeqNumberGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return leftPart.containsSameValueAs(other.leftPart)
                && rightPart.containsSameValueAs(other.rightPart)

    }


    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        log.trace("Randomizing ${this::class.java.simpleName}")
        val genes: List<Gene> = listOf(leftPart, rightPart)
        val index = randomness.nextInt(genes.size)
        genes[index].randomize(randomness, forceNewValue, allGenes)
    }

    /**
     * Forbid explicitly individual mutation
     * of these genes
     */
    override fun candidatesInternalGenes(
            randomness: Randomness,
            apc: AdaptiveParameterControl,
            allGenes: List<Gene>,
            selectionStrategy: SubsetGeneSelectionStrategy,
            enableAdaptiveGeneMutation: Boolean,
            additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): List<Gene> {
        return listOf()
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



    override fun innerGene(): List<Gene> =
            listOf(leftPart, rightPart)

    override fun bindValueBasedOn(gene: Gene): Boolean {
        if (gene is SqlLogSeqNumberGene) {
            this.leftPart.bindValueBasedOn(gene.leftPart)
            this.rightPart.bindValueBasedOn(gene.rightPart)
        }
        LoggingUtil.uniqueWarn(
                log,
                "cannot bind ${this::class.java.simpleName} with ${gene::class.java.simpleName}"
        )
        return false
    }


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