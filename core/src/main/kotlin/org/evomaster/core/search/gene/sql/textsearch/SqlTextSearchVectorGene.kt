package org.evomaster.core.search.gene.sql.textsearch

import org.evomaster.core.search.gene.*
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.GeneUtils.replaceEnclosedQuotationMarksWithSingleApostrophePlaceHolder
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This gene represents values of a ts_vector column in postgres.
 * In order to create such values, function to_tsvector() must be invoked.
 * For example,
 *   ts_vector('')
 *   ts_vector('foo bar')
 *
 *  are valid values for a ts_vector column.
 */
class SqlTextSearchVectorGene(
        name: String,
        private val textLexeme: StringGene = StringGene(name = "textLexemes")
) : CompositeFixedGene(name, mutableListOf(textLexeme)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlTextSearchVectorGene::class.java)

        const val TO_TSVECTOR = "to_tsvector"
    }

    override fun copyContent(): Gene = SqlTextSearchVectorGene(
            name,
            textLexeme.copyContent() as StringGene
    )

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        textLexeme.randomize(randomness, forceNewValue, allGenes)
    }

    override fun candidatesInternalGenes(
            randomness: Randomness,
            apc: AdaptiveParameterControl,
            allGenes: List<Gene>,
            selectionStrategy: SubsetGeneSelectionStrategy,
            enableAdaptiveGeneMutation: Boolean,
            additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): List<Gene> {
        return listOf(textLexeme)
    }


    override fun getValueAsPrintableString(
            previousGenes: List<Gene>,
            mode: GeneUtils.EscapeMode?,
            targetFormat: OutputFormat?,
            extraCheck: Boolean
    ): String {
        val str = replaceEnclosedQuotationMarksWithSingleApostrophePlaceHolder(
                textLexeme.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck))
        return "${TO_TSVECTOR}(${str})"
    }

    override fun getValueAsRawString(): String {
        return textLexeme.getValueAsRawString()
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlTextSearchVectorGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.textLexeme.copyValueFrom(other.textLexeme)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlTextSearchVectorGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.textLexeme.containsSameValueAs(other.textLexeme)
    }


    override fun innerGene(): List<Gene> = listOf(textLexeme)

    override fun bindValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is SqlTextSearchVectorGene -> {
                textLexeme.bindValueBasedOn(gene.textLexeme)
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind PathGene with ${gene::class.java.simpleName}")
                false
            }
        }
    }


}