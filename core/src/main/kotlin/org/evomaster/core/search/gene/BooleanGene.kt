package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.MutationWeightControl
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneSelectionInfo
import org.evomaster.core.search.service.mutator.genemutation.IntMutationUpdate
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy


class BooleanGene(
        name: String,
        var value: Boolean = true,
        val valueMutationInfo : IntMutationUpdate = IntMutationUpdate(0, 1)
) : Gene(name) {


    override fun copy(): Gene {
        return BooleanGene(name, value, valueMutationInfo.copy())
    }

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {

        val k: Boolean = if (forceNewValue) {
            !value
        } else {
            randomness.nextBoolean()
        }

        value = k
    }

    override fun mutate(randomness: Randomness, apc: AdaptiveParameterControl, mwc: MutationWeightControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneSelectionInfo?): Boolean {
        value = ! value
        return true
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
        return value.toString()
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is BooleanGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.value = other.value
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is BooleanGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.value == other.value
    }
}