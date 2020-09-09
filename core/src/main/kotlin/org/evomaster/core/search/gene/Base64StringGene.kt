package org.evomaster.core.search.gene

import org.evomaster.core.output.OutputFormat
import org.evomaster.core.search.service.AdaptiveParameterControl
import org.evomaster.core.search.service.Randomness
import org.evomaster.core.search.service.mutator.EvaluatedMutation
import org.evomaster.core.search.service.mutator.genemutation.AdditionalGeneMutationInfo
import org.evomaster.core.search.service.mutator.genemutation.ArchiveGeneMutator
import org.evomaster.core.search.service.mutator.genemutation.SubsetGeneSelectionStrategy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*


class Base64StringGene(
        name: String,
        val data: StringGene = StringGene("data")
) : Gene(name) {

    companion object{
        val log : Logger = LoggerFactory.getLogger(Base64StringGene::class.java)
    }

    init {
        data.parent = this
    }

    override fun copy(): Gene = Base64StringGene(name, data.copy() as StringGene)

    override fun randomize(randomness: Randomness, forceNewValue: Boolean, allGenes: List<Gene>) {
        data.randomize(randomness, forceNewValue)
    }

    override fun candidatesInternalGenes(randomness: Randomness, apc: AdaptiveParameterControl, allGenes: List<Gene>, selectionStrategy: SubsetGeneSelectionStrategy, enableAdaptiveGeneMutation: Boolean, additionalGeneMutationInfo: AdditionalGeneMutationInfo?): List<Gene> {
        return listOf(data)
    }

    override fun getValueAsPrintableString(previousGenes: List<Gene>, mode: GeneUtils.EscapeMode?, targetFormat: OutputFormat?): String {
        return Base64.getEncoder().encodeToString(data.value.toByteArray())
    }

    override fun copyValueFrom(other: Gene) {
        if (other !is Base64StringGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        this.data.copyValueFrom(other.data)
    }

    override fun containsSameValueAs(other: Gene): Boolean {
        if (other !is Base64StringGene) {
            throw IllegalArgumentException("Invalid gene type ${other.javaClass}")
        }
        return this.data.containsSameValueAs(other.data)
    }


    override fun flatView(excludePredicate: (Gene) -> Boolean): List<Gene>{
        return if(excludePredicate(this)) listOf(this) else listOf(this).plus(data.flatView(excludePredicate))
    }

    override fun innerGene(): List<Gene> = listOf()
}