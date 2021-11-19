package org.evomaster.core.search.gene.geometric

import org.evomaster.core.search.gene.*
import org.evomaster.core.logging.LoggingUtil
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory

abstract class PQGeometricAbstractGene(
    name: String,
    protected val p: PointGene,
    protected val q: PointGene,
    val doNotAllowSamePoints: Boolean = false
) : Gene(name, mutableListOf(p, q)) {

    companion object {
        val log: Logger = LoggerFactory.getLogger(PQGeometricAbstractGene::class.java)
    }

    override fun getChildren(): MutableList<Gene> = mutableListOf(p, q)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        p.randomize(randomness, forceNewValue, allGenes)
        q.randomize(randomness, forceNewValue, allGenes)
    }

    override fun candidatesInternalGenes(
        randomness: Randomness,
        apc: AdaptiveParameterControl,
        allGenes: List<Gene>,
        selectionStrategy: SubsetGeneSelectionStrategy,
        enableAdaptiveGeneMutation: Boolean,
        additionalGeneMutationInfo: AdditionalGeneMutationInfo?
    ): List<Gene> {
        return listOf(p, q)
    }

    override fun getValueAsPrintableString(
        previousGenes: List<Gene>,
        mode: GeneUtils.EscapeMode?,
        targetFormat: OutputFormat?,
        extraCheck: Boolean
    ): String {
        return "\" ( ${p.getValueAsRawString()} , ${q.getValueAsRawString()} ) \""
    }

    override fun getValueAsRawString(): String {
        return "( ${p.getValueAsRawString()} , ${q.getValueAsRawString()} )"
    }


    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this) else
            listOf(this).plus(p.flatView(excludePredicate)).plus(q.flatView(excludePredicate))
    }

    override fun innerGene(): List<Gene> = listOf(p, q)


}