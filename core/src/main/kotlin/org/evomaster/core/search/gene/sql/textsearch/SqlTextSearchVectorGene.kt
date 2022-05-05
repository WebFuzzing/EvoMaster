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

class SqlTextSearchVectorGene(
    name: String,
    val textLexemes: ArrayGene<StringGene> = ArrayGene(name = "textLexemes", template = StringGene("p"))
) : Gene(name, mutableListOf(textLexemes)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlTextSearchVectorGene::class.java)
    }

    override fun getChildren(): MutableList<Gene> = mutableListOf(textLexemes)

    override fun copyContent(): Gene = SqlTextSearchVectorGene(
        name,
        textLexemes.copyContent() as ArrayGene<StringGene>
    )

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        textLexemes.randomize(randomness, forceNewValue, allGenes)
        /*
         *  A geometric polygon must be always a non-empty list
         */
        if (textLexemes.getAllElements().isEmpty()) {
            val stringGene = StringGene("lexeme")
            stringGene.randomize(randomness, forceNewValue, allGenes)
            textLexemes.addElement(stringGene)
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
        return listOf(textLexemes)
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return textLexemes.getAllElements()
            .map { it.getValueAsPrintableString(previousGenes, mode, targetFormat, extraCheck) }
            .joinToString(" ")

    }

    override fun getValueAsRawString(): String {
        return textLexemes.getAllElements()
            .map { it.getValueAsRawString() }
            .joinToString(" , ")

    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlTextSearchVectorGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.textLexemes.copyValueFrom(other.textLexemes)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlTextSearchVectorGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.textLexemes.containsSameValueAs(other.textLexemes)
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(textLexemes.flatView(excludePredicate))
    }

    override fun innerGene(): List<Gene> = listOf(textLexemes)

    override fun bindValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is SqlTextSearchVectorGene -> {
                textLexemes.bindValueBasedOn(gene.textLexemes)
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind PathGene with ${gene::class.java.simpleName}")
                false
            }
        }
    }


}