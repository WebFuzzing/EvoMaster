package org.evomaster.core.search.gene.regex

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.GeneUtils
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy

/**
 * Immutable class
 */
class PatternCharacterBlock(
        name: String,
        val stringBlock: String
) : RxAtom(name, listOf()) {

    override fun isMutable(): Boolean {
        return false
    }

    override fun getChildren(): List<Gene> = listOf()

    override fun copyContent(): Gene {
        return PatternCharacterBlock(name, stringBlock)
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        throw IllegalStateException("Not supposed to mutate " + this.javaClass.simpleName)
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        throw IllegalStateException("Not supposed to mutate " + this.javaClass.simpleName)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return stringBlock
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is PatternCharacterBlock) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        if(other.stringBlock != this.stringBlock) {
            //this should not happen
            throw IllegalStateException("Not supposed to copy value for " + this.javaClass.simpleName)
        }
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is PatternCharacterBlock) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.stringBlock == other.stringBlock
    }

    override fun innerGene(): List<Gene> = listOf()

    override fun bindValueBasedOn(gene: Gene): Boolean {
        // do nothing
        return true
    }
}