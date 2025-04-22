package org.evomaster.core.search.gene.regex

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.gene.Gene
import org.evomaster.core.search.gene.utils.GeneUtils
import org.evomaster.core.search.gene.root.SimpleGene
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneMutationSelectionStrategy

/**
 * Immutable class
 */
class PatternCharacterBlockGene(
        name: String,
        val stringBlock: String
) : RxAtom, SimpleGene(name) {

    override fun isMutable(): Boolean {
        return false
    }

    override fun checkForLocallyValidIgnoringChildren() : Boolean{
        return true
    }

    override fun copyContent(): Gene {
        return PatternCharacterBlockGene(name, stringBlock)
    }

    override fun setValueWithRawString(value: String) {
        throw IllegalStateException("cannot set value with string ($value) for ${this.javaClass.simpleName}")
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        throw IllegalStateException("Not supposed to mutate " + this.javaClass.simpleName)
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneMutationSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        throw IllegalStateException("Not supposed to mutate " + this.javaClass.simpleName)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        return stringBlock
    }

    override fun copyValueFrom(other: Gene): Boolean {
        if (other !is PatternCharacterBlockGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }

        if(other.stringBlock != this.stringBlock) {
            //this should not happen
            throw IllegalStateException("Not supposed to copy value for " + this.javaClass.simpleName)
        }

        return true
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is PatternCharacterBlockGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.stringBlock == other.stringBlock
    }


    override fun setValueBasedOn(gene: Gene): Boolean {
        // do nothing
        return true
    }
}