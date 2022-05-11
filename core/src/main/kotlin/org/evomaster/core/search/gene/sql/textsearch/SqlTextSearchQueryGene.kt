package org.evomaster.core.search.gene.sql.textsearch

import org.evomaster.core.search.gene.*
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.GeneUtils.SINGLE_APOSTROPHE_PLACEHOLDER
import org.evomaster.core.search.gene.GeneUtils.removeEnclosedQuotationMarks
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
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

) : Gene(name, mutableListOf(queryLexemes)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlTextSearchQueryGene::class.java)
        const val TO_TSQUERY = "to_tsquery"

        const val AMPERSAND_CHAR = '&'

        const val BLANK_CHAR = ' '
    }

    override fun copyContent(): Gene = SqlTextSearchQueryGene(
            name,
            queryLexemes.copyContent() as ArrayGene<StringGene>
    )

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        queryLexemes.randomize(randomness, forceNewValue, allGenes)
        /*
         *  A geometric polygon must be always a non-empty list
         */
        if (queryLexemes.getAllElements().isEmpty()) {
            val stringGene = StringGene("lexeme")
            stringGene.randomize(randomness, forceNewValue, allGenes)
            queryLexemes.addElement(stringGene)
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
        return listOf(queryLexemes)
    }

    override fun getValueAsPrintableString(
            previousGenes: List<Gene>,
            mode: GeneUtils.EscapeMode?,
            targetFormat: OutputFormat?,
            extraCheck: Boolean
    ): String {
        val queryStr =
                queryLexemes.getAllElements()
                        .map {
                            removeEnclosedQuotationMarks(
                                    it.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck))
                        }
                        .joinToString(" $AMPERSAND_CHAR ")
        return "${TO_TSQUERY}(${SINGLE_APOSTROPHE_PLACEHOLDER + queryStr + SINGLE_APOSTROPHE_PLACEHOLDER})"
    }

    override fun getValueAsRawString(): String {
        return queryLexemes.getAllElements()
                .map { it.getValueAsRawString() }
                .joinToString(" $AMPERSAND_CHAR ")

    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlTextSearchQueryGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.queryLexemes.copyValueFrom(other.queryLexemes)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlTextSearchQueryGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.queryLexemes.containsSameValueAs(other.queryLexemes)
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(queryLexemes.flatView(excludePredicate))
    }

    override fun innerGene(): List<Gene> = listOf(queryLexemes)

    override fun bindValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is SqlTextSearchQueryGene -> {
                queryLexemes.bindValueBasedOn(gene.queryLexemes)
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind PathGene with ${gene::class.java.simpleName}")
                false
            }
        }
    }


}