package org.evomaster.core.search.gene.sql.textsearch

import org.evomaster.core.search.gene.*
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SqlTextSearchQueryGene(
    name: String,
    val queryLexemes: ArrayGene<StringGene> = ArrayGene(name = "lexemes", template = StringGene("p"))
) : Gene(name, mutableListOf(queryLexemes)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlTextSearchQueryGene::class.java)
    }

    init {
        /*
         * Text Search vectors must be non-empty lists
         */
        queryLexemes.addElement(StringGene("lexeme"))
    }

    override fun getChildren(): MutableList<Gene> = mutableListOf(queryLexemes)

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
        return queryLexemes.getAllElements()
            .map { it.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck) }
            .joinToString(" & ")

    }

    override fun getValueAsRawString(): String {
        return queryLexemes.getAllElements()
                .map { it.getValueAsRawString() }
                .joinToString(" & ")

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