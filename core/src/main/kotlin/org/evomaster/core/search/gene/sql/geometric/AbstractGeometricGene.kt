package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.core.search.gene.*
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy

abstract class AbstractGeometricGene(
    name: String,
    protected val p: SqlPointGene,
    protected val q: SqlPointGene,
    val doNotAllowSamePoints: Boolean = false
) : Gene(name, mutableListOf(p, q)) {



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