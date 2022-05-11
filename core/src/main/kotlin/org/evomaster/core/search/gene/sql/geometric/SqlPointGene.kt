package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.core.search.gene.*
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class SqlPointGene(
    name: String,
    val x: FloatGene = FloatGene(name = "x"),
    val y: FloatGene = FloatGene(name = "y")
) : Gene(name, mutableListOf(x, y)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(SqlPointGene::class.java)
    }


    override fun copyContent(): Gene = SqlPointGene(
        name,
        x.copyContent(),
        y.copyContent()
    )

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        x.randomize(randomness, forceNewValue, allGenes)
        y.randomize(randomness, forceNewValue, allGenes)
    }

    override fun candidatesInternalGenes(
        randomness: Randomness,
        apc: AdaptiveParameterControl,
        allGenes: List<Gene>,
        selectionStrategy: SubsetGeneSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): List<Gene> {
        return listOf(x, y)
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return "\" (${x.getValueAsRawString()} , ${y.getValueAsRawString()}) \""
    }

    override fun getValueAsRawString(): String {
        return "(${x.getValueAsRawString()} , ${y.getValueAsRawString()})"
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is SqlPointGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.x.copyValueFrom(other.x)
        this.y.copyValueFrom(other.y)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is SqlPointGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.x.containsSameValueAs(other.x)
                && this.y.containsSameValueAs(other.y)
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(x.flatView(excludePredicate)).plus(y.flatView(excludePredicate))
    }

    override fun innerGene(): List<Gene> = listOf(x, y)

    override fun bindValueBasedOn(gene: Gene): Boolean {
        return when {
            gene is SqlPointGene -> {
                x.bindValueBasedOn(gene.x) &&
                        y.bindValueBasedOn(gene.y)
            }
            else -> {
                LoggingUtil.uniqueWarn(log, "cannot bind PointGene with ${gene::class.java.simpleName}")
                false
            }
        }
    }


}