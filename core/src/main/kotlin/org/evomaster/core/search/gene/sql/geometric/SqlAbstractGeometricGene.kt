package org.evomaster.core.search.gene.sql.geometric

import org.evomaster.core.search.gene.*
import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy

abstract class SqlAbstractGeometricGene(
    name: String,
    val p: SqlPointGene,
    val q: SqlPointGene
) : CompositeFixedGene(name, mutableListOf(p, q)) {



    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean, allGenes: List<Gene>) {
        p.randomize(randomness, tryToForceNewValue, allGenes)
        q.randomize(randomness, tryToForceNewValue, allGenes)
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
        return "\"${getValueAsRawString()}\""
    }

    override fun getValueAsRawString(): String {
        return "(${p.getValueAsRawString()}, ${q.getValueAsRawString()})"
    }




    override fun innerGene(): List<Gene> = listOf(p, q)


}