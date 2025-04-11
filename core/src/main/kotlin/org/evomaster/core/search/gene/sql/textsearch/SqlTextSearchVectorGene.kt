package org.evomaster.core.search.gene.sql.textsearch

import org.evomaster.core.search.gene.*
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.utils.GeneUtils.replaceEnclosedQuotationMarksWithSingleApostrophePlaceHolder
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
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

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun copyContent(): Gene = SqlTextSearchVectorGene(
            name,
            textLexeme.copy() as StringGene
    )

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        textLexeme.randomize(randomness, tryToForceNewValue)
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

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is SqlTextSearchVectorGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return updateValueOnlyIfValid(
            {this.textLexeme.copyValueFrom(other.textLexeme)}, false
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlTextSearchVectorGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.textLexeme.containsSameValueAs(other.textLexeme)
    }


    override fun setValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is SqlTextSearchVectorGene -> {
                textLexeme.setValueBasedOn(gene.textLexeme)
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind PathGene with ${gene::class.java.simpleName}")
                false
            }
        }
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