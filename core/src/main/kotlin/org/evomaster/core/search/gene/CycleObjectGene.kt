package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.ArchiveGeneMutator
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy

/**
 * It might happen that object A has reference to B,
 * and B has reference to A', where A' might or might
 * not be equal to A.
 * In this case, we cannot represent A'.
 *
 * TODO need to handle cases when some of those are
 * marked with "required"
 *
 * Note that for [CycleObjectGene], its [refType] is null
 */
class CycleObjectGene(name: String) : ObjectGene(name, listOf()) {

    override fun isMutable() = false

    override fun isLocallyValid() : Boolean{
        return true
    }
    override fun copyContent(): Gene = CycleObjectGene(name)

    override fun randomize(randomness: Randomness, tryToForceNewValue: Boolean) {
        //nothing to do
    }

    override fun shallowMutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl,  selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): Boolean {
        randomize(randomness, true)
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?, extraCheck: Boolean): String {
        throw IllegalStateException("CycleObjectGene has no value")
    }

    override fun innerGene(): List<Gene> = listOf()

    override fun isPrintable(): Boolean {
        return false
    }
}