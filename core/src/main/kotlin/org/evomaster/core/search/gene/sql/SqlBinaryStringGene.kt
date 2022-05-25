package org.evomaster.core.search.gene.sql

import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.StructuralElement
import org.evomaster.core.search.gene.*
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Binary strings are strings of 1's and 0's.
 */
class SqlBinaryStringGene(
        /**
         * The name of this gene
         */
        name: String,

        val minSize: Int = 0,

        val maxSize: Int = ArrayGene.MAX_SIZE,

        private val binaryArrayGene: ArrayGene<IntegerGene> = ArrayGene(name, template = IntegerGene(name, min = 0, max = 255), minSize = minSize, maxSize = maxSize)

) :  CompositeGene(name, mutableListOf( binaryArrayGene)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlBinaryStringGene::class.java)

        const val EMPTY_STR = ""
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        binaryArrayGene.randomize(randomness, forceNewValue, allGenes)
    }

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): List<Gene> {
        TODO("Not yet implemented")
    }

    private fun toHex2(value: Int) = value.toString(16).padStart(2, '0')


    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return buildString {
            append("\"\\x")
            append(binaryArrayGene.getViewOfChildren()
                    .map { g ->
                toHex2((g as IntegerGene).value)
            }.joinToString(EMPTY_STR))
            append("\"")
        }
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlBinaryStringGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        binaryArrayGene.copyValueFrom(other.binaryArrayGene)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlBinaryStringGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return binaryArrayGene.containsSameValueAs(other.binaryArrayGene)
    }

    override fun innerGene(): List<Gene> {
        return listOf(binaryArrayGene)
    }

    override fun bindValueBasedOn(gene: Gene): Boolean {
        if (gene is SqlBinaryStringGene) {
            return binaryArrayGene.bindValueBasedOn(gene.binaryArrayGene)
        }
        LoggingUtil.uniqueWarn(log, "cannot bind SqlBitstringGene with ${gene::class.java.simpleName}")
        return false
    }

    override fun copyContent() = SqlBinaryStringGene(name, minSize = minSize, maxSize = maxSize, binaryArrayGene.copy() as ArrayGene<IntegerGene>)

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