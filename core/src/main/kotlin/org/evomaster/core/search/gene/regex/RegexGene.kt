package org.evomaster.core.search.gene.regex

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.geneMutation.AdditionalGeneSelectionInfo
import org.evomaster.core.search.service.mutator.geneMutation.SubsetGeneSelectionStrategy

/**
 * A gene representing a regular expression (regex).
 */
class RegexGene(
        name: String,
        val disjunctions: DisjunctionListRxGene
) : Gene(name) {

    init {
        disjunctions.parent = this
    }

    override fun copy(): Gene {
        return RegexGene(name, disjunctions.copy() as DisjunctionListRxGene)
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        disjunctions.randomize(randomness, forceNewValue, allGenes)
    }

    override fun isMutable(): Boolean {
        return disjunctions.isMutable()
    }

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?): List<Gene> {
        return if (isMutable()) emptyList() else listOf(disjunctions)
    }

    override fun adaptiveSelectSubset(internalGenes: List<Gene>, mwc: MutationWeightControl, additionalGeneMutationInfo: AdditionalGeneSelectionInfo): Map<Gene, AdditionalGeneSelectionInfo?> {
        TODO()
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?): Boolean {
        // do nothing since disjunctions is not mutable
        return true
    }

    override fun getValueAsRawString(): String {
        return disjunctions.getValueAsPrintableString(targetFormat = null)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
        val rawValue = getValueAsRawString()
        when {
            // TODO Should refactor since this code block is equivalent to StringGene.getValueAsPrintableString()
            /*(targetFormat == null) -> return "\"$rawValue\""
            targetFormat.isKotlin() -> return "\"$rawValue\""
                    .replace("\\", "\\\\")
                    .replace("$", "\\$")
            else -> return "\"$rawValue\""
                    .replace("\\", "\\\\")
             */
            (targetFormat == null) -> return "\"${rawValue}\""
            //"\"${rawValue.replace("\"", "\\\"")}\""
            (mode != null) -> return "\"${GeneUtils.applyEscapes(rawValue, mode, targetFormat)}\""
            else -> return "\"${GeneUtils.applyEscapes(rawValue, GeneUtils.EscapeMode.TEXT ,targetFormat)}\""
        }
    }


    override fun copyValueFrom(other: Gene) {
        if(other !is RegexGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.disjunctions.copyValueFrom(other.disjunctions)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if(other !is RegexGene){
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.disjunctions.containsSameValueAs(other.disjunctions)
    }

    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene> {
        return if (excludePredicate(this)) listOf(this)
        else listOf(this).plus(disjunctions.flatView(excludePredicate))
    }

    /**
     * use mutationweight of [disjunctions]
     */
    override fun mutationWeight(): Int = disjunctions.mutationWeight()
}