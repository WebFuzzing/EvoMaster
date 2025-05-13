package org.evomaster.core.search.gene.sql.textsearch

import org.evomaster.core.search.gene.*
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.collection.ArrayGene
import org.evomaster.core.search.gene.root.CompositeFixedGene
import org.evomaster.core.search.gene.string.StringGene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.utils.GeneUtils.SINGLE_APOSTROPHE_PLACEHOLDER
import org.evomaster.core.search.gene.utils.GeneUtils.removeEnclosedQuotationMarks
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * This gene represents a query value of a ts_query column in postgres.
 * A query is a list of strings separated by & ('foo & bar').
 * In order to create this type the to_tsquery() function must be invoked.
 * For instance.
 *  ts_query('')
 *  ts_query('foo bar')
 *  ts_query('foo & bar')
 *
 *  are all valid ts_query values.
 *
 *  In contrast, operand must not be used with blank lexemes.
 *  ts_query(' & ')
 *  ts_query('foo & ')
 *  ts_qyert(' & bar')
 *
 *  are NOT valid ts_query values
 */
class SqlTextSearchQueryGene(
    /*
     * The name of this gene
     */
        name: String,
    /*
     * TS queries are lists of lexemes.
     */
        val queryLexemes: ArrayGene<StringGene> = ArrayGene(name = "lexemes",
                template = StringGene("lexeme template",
                        // lexemes are non empty strings
                        minLength = 1,
                        // lexemes do not contain '&' or ' ' (blank characters)
                        invalidChars = listOf(BLANK_CHAR, AMPERSAND_CHAR)),
                // the list of lexemes is empty to represent the valid '' query
                minSize = 0
                ),

    ) : CompositeFixedGene(name, mutableListOf(queryLexemes)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlTextSearchQueryGene::class.java)
        const val TO_TSQUERY = "to_tsquery"

        const val AMPERSAND_CHAR = '&'

        const val BLANK_CHAR = ' '
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun copyContent(): Gene = SqlTextSearchQueryGene(
            name,
            queryLexemes.copy() as ArrayGene<StringGene>
    )

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        queryLexemes.randomize(randomness, tryToForceNewValue)
        /*
         *  A geometric polygon must be always a non-empty list
         */
        if (queryLexemes.getViewOfElements().isEmpty()) {
            val stringGene = StringGene("lexeme")
            stringGene.randomize(randomness, tryToForceNewValue)
            queryLexemes.addElement(stringGene)
        }
    }



    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        val queryStr =
                queryLexemes.getViewOfElements()
                        .map {
                            removeEnclosedQuotationMarks(
                                    it.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck))
                        }
                        .joinToString(" $AMPERSAND_CHAR ")
        return "${TO_TSQUERY}(${SINGLE_APOSTROPHE_PLACEHOLDER + queryStr + SINGLE_APOSTROPHE_PLACEHOLDER})"
    }

    override fun getValueAsRawString(): String {
        return queryLexemes.getViewOfElements()
                .map { it.getValueAsRawString() }
                .joinToString(" $AMPERSAND_CHAR ")

    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is SqlTextSearchQueryGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return updateValueOnlyIfValid(
            {this.queryLexemes.copyValueFrom(other.queryLexemes)}, false
        )
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlTextSearchQueryGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.queryLexemes.containsSameValueAs(other.queryLexemes)
    }


    override fun setValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is SqlTextSearchQueryGene -> {
                queryLexemes.setValueBasedOn(gene.queryLexemes)
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