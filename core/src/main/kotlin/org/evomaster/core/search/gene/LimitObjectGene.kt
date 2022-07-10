package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy


/**
 * This gene is mainly created for GraphQL.
 * It is used as a placeholder when a certain limit is reached
 */

class LimitObjectGene(name: String) : ObjectGene(name, listOf()) {

    override fun isMutable() = false

    override fun copyContent(): Gene = LimitObjectGene(name)

    override fun isLocallyValid() : Boolean{
        return true
    }

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        //nothing to do
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        randomize(randomness, true)
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        throw IllegalStateException("LimitObjectGene has no value")
    }

    override fun innerGene(): List<Gene> = listOf()

    override fun isPrintable(): Boolean {
        return false
    }
}